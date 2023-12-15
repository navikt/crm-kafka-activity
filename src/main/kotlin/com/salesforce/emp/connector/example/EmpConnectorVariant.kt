package com.salesforce.emp.connector.example

import com.salesforce.emp.connector.CannotSubscribe
import com.salesforce.emp.connector.ReplayExtension
import com.salesforce.emp.connector.TopicSubscription
import org.cometd.bayeux.Channel
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.LongPollingTransport
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Function
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @author hal.hildebrand
 * @since API v37.0
 * modified by bjorn.hagglund
 */
class EmpConnectorVariant {
    private inner class SubscriptionImpl internal constructor(
        private val topic: String,
        private val consumer: Consumer<Map<String, Any>>
    ) :
        TopicSubscription {
        init {
            subscriptions.add(this)
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#cancel()
         */
        override fun cancel() {
            replay.remove(topicWithoutQueryString(topic))
            if (running.get() && client != null) {
                client!!.getChannel(topic).unsubscribe()
                subscriptions.remove(this)
            }
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#getReplay()
         */
        override fun getReplayFrom(): Long {
            return replay.getOrDefault(topicWithoutQueryString(topic), REPLAY_FROM_EARLIEST)!!
        }

        /*
         * (non-Javadoc)
         * @see com.salesforce.emp.connector.Subscription#getTopic()
         */
        override fun getTopic(): String {
            return topic
        }

        override fun toString(): String {
            return String.format("Subscription [%s:%s]", getTopic(), replayFrom)
        }

        fun subscribe(): Future<TopicSubscription> {
            val replayFrom = replayFrom
            val channel = client!!.getChannel(topic)
            val future = CompletableFuture<TopicSubscription>()
            channel.subscribe(
                { _: ClientSessionChannel, message: Message ->
                    consumer.accept(
                        message.dataAsMap
                    )
                }
            ) { _: ClientSessionChannel, message: Message ->
                if (message.isSuccessful) {
                    future.complete(this)
                } else {
                    var error = message[ERROR]
                    if (error == null) {
                        error = message[FAILURE]
                    }
                    future.completeExceptionally(
                        CannotSubscribe(
                            parameters.endpoint(),
                            topic,
                            replayFrom,
                            error ?: message
                        )
                    )
                }
            }
            return future
        }
    }

    @Volatile
    private var client: BayeuxClient? = null
    private val httpClient: HttpClient?
    private lateinit var bayeuxClientFn: (String, LongPollingTransport) -> BayeuxClient
    private val parameters: BayeuxParametersVariant
    private val replay: ConcurrentMap<String, Long> = ConcurrentHashMap()
    private val running = AtomicBoolean()
    private val subscriptions: MutableSet<SubscriptionImpl> = CopyOnWriteArraySet()
    private val listenerInfos: MutableSet<MessageListenerInfo> = CopyOnWriteArraySet()
    private var bearerTokenProvider: Function<Boolean, String>? = null
    private val reauthenticate = AtomicBoolean(false)

    constructor(
        parameters: BayeuxParametersVariant,
        httpClientFn: (SslContextFactory) -> HttpClient,
        bayeuxClientFn: (String, LongPollingTransport) -> BayeuxClient
    ) {
        this.parameters = parameters
        val ctxFactory = parameters.sslContextFactory()
        httpClient = httpClientFn(ctxFactory)
        httpClient.proxyConfiguration.proxies.addAll(parameters.proxies())
        this.bayeuxClientFn = bayeuxClientFn
    }

    constructor(parameters: BayeuxParametersVariant, httpClient: HttpClient?) {
        this.parameters = parameters
        this.httpClient = httpClient
        this.httpClient!!.proxyConfiguration.proxies.addAll(parameters.proxies())
    }

    /**
     * Start the connector.
     * @return true if connection was established, false otherwise
     */
    fun start(): Future<Boolean> {
        if (running.compareAndSet(false, true)) {
            addListener(Channel.META_CONNECT, AuthFailureListener())
            addListener(Channel.META_HANDSHAKE, AuthFailureListener())
            replay.clear()
            return connect()
        }
        val future = CompletableFuture<Boolean>()
        future.complete(true)
        return future
    }

    /**
     * Stop the connector
     */
    fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }
        if (client != null) {
            log.info("Disconnecting Bayeux Client in EmpConnector")
            client!!.disconnect()
            client = null
        }
        if (httpClient != null) {
            try {
                httpClient.stop()
            } catch (e: Exception) {
                log.error("Unable to stop HTTP transport[{}]", parameters.endpoint(), e)
            }
        }
    }

    /**
     * Set a bearer token / session id provider function that takes a boolean as input and returns a valid token.
     * If the input is true, the provider function is supposed to re-authenticate with the Salesforce server
     * and get a fresh session id or token.
     *
     * @param bearerTokenProvider a bearer token provider function.
     */
    fun setBearerTokenProvider(bearerTokenProvider: Function<Boolean, String>) {
        this.bearerTokenProvider = bearerTokenProvider
    }

    /**
     * Subscribe to a topic, receiving events after the replayFrom position
     *
     * @param topic
     * - the topic to subscribe to
     * @param replayFrom
     * - the replayFrom position in the event stream
     * @param consumer
     * - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     * exception
     */
    fun subscribe(
        topicIn: String,
        replayFrom: Long,
        consumer: Consumer<Map<String, Any>>
    ): Future<TopicSubscription> {
        var topic = topicIn
        check(running.get()) {
            String.format(
                "Connector[%s} has not been started",
                parameters.endpoint()
            )
        }
        topic = topic.replace("/$".toRegex(), "")
        val topicWithoutQueryString = topicWithoutQueryString(topic)
        check(replay.putIfAbsent(topicWithoutQueryString, replayFrom) == null) {
            String.format(
                "Already subscribed to %s [%s]",
                topic,
                parameters.endpoint()
            )
        }
        val subscription: SubscriptionImpl = SubscriptionImpl(topic, consumer)
        return subscription.subscribe()
    }

    /**
     * Subscribe to a topic, receiving events from the earliest event position in the stream
     *
     * @param topic
     * - the topic to subscribe to
     * @param consumer
     * - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     * exception
     */
    fun subscribeEarliest(topic: String, consumer: Consumer<Map<String, Any>>): Future<TopicSubscription> {
        return subscribe(topic, REPLAY_FROM_EARLIEST, consumer)
    }

    /**
     * Subscribe to a topic, receiving events from the latest event position in the stream
     *
     * @param topic
     * - the topic to subscribe to
     * @param consumer
     * - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     * exception
     */
    fun subscribeTip(topic: String, consumer: Consumer<Map<String, Any>>): Future<TopicSubscription> {
        return subscribe(topic, REPLAY_FROM_TIP, consumer)
    }

    fun addListener(channel: String, messageListener: ClientSessionChannel.MessageListener): EmpConnectorVariant {
        listenerInfos.add(MessageListenerInfo(channel, messageListener))
        return this
    }

    val isConnected: Boolean
        get() = client != null && client!!.isConnected
    val isDisconnected: Boolean
        get() = client == null || client!!.isDisconnected
    val isHandshook: Boolean
        get() = client != null && client!!.isHandshook

    fun getLastReplayId(topic: String?): Long {
        return replay[topic]!!
    }

    private fun connect(): Future<Boolean> {
        log.info("EmpConnector connecting")
        val future = CompletableFuture<Boolean>()
        try {
            httpClient!!.start()
        } catch (e: Exception) {
            log.error("Unable to start HTTP transport[{}]", parameters.endpoint(), e)
            running.set(false)
            future.complete(false)
            return future
        }
        val bearerToken = bearerToken()
        val httpTransport: LongPollingTransport =
            object : LongPollingTransport(parameters.longPollingOptions(), httpClient) {
                override fun customize(request: Request) {
                    request.header(AUTHORIZATION, bearerToken)
                }
            }
        client = bayeuxClientFn(parameters.endpoint().toExternalForm(), httpTransport)
        client!!.addExtension(ReplayExtension(replay))
        addListeners(client!!)
        client!!.handshake { c: ClientSessionChannel, m: Message ->
            if (!m.isSuccessful) {
                log.warn("Report unsuccessful handshake")
                var error = m[ERROR]
                if (error == null) {
                    error = m[FAILURE]
                }
                future.completeExceptionally(
                    ConnectException(
                        String.format(
                            "Cannot connect at handshake [%s] : %s",
                            parameters.endpoint(),
                            error
                        )
                    )
                )
                running.set(false)
            } else {
                subscriptions.forEach(Consumer { obj: SubscriptionImpl -> obj.subscribe() })
                future.complete(true)
            }
        }
        return future
    }

    private fun addListeners(client: BayeuxClient) {
        for (info in listenerInfos) {
            client.getChannel(info.channelName).addListener(info.messageListener)
        }
    }

    private fun bearerToken(): String {
        val bearerToken: String
        if (bearerTokenProvider != null) {
            bearerToken = bearerTokenProvider!!.apply(reauthenticate.get())
            reauthenticate.compareAndSet(true, false)
        } else {
            bearerToken = parameters.bearerToken()
        }
        return bearerToken
    }

    private fun reconnect() {
        if (running.compareAndSet(false, true)) {
            connect()
        } else {
            log.error("The current value of running is not as we expect, this means our reconnection may not happen")
        }
    }

    /**
     * Listens to /meta/connect channel messages and handles 401 errors, where client needs
     * to reauthenticate.
     */
    private inner class AuthFailureListener : ClientSessionChannel.MessageListener {
        override fun onMessage(channel: ClientSessionChannel, message: Message) {
            if (!message.isSuccessful) {
                if (isError(message, "401") || isError(message, "403")) {
                    log.info("Received authentication failure - will reauthenticate")
                    reauthenticate.set(true)
                    stop()
                    reconnect()
                }
            }
        }

        private fun isError(message: Message, errorCode: String): Boolean {
            val error = message[Message.ERROR_FIELD] as String?
            val failureReason = getFailureReason(message)
            return error != null && error.startsWith(errorCode) || failureReason != null && failureReason.startsWith(
                errorCode
            )
        }

        private fun getFailureReason(message: Message): String? {
            var failureReason: String? = null
            val ext = message.ext
            if (ext != null) {
                val sfdc = ext["sfdc"] as Map<String, Any>
                if (sfdc != null) {
                    failureReason = sfdc["failureReason"] as String?
                }
            }
            return failureReason
        }
    }

    private class MessageListenerInfo internal constructor(
        val channelName: String?,
        val messageListener: ClientSessionChannel.MessageListener?
    )

    companion object {
        private const val ERROR = "error"
        private const val FAILURE = "failure"
        var REPLAY_FROM_EARLIEST = -2L
        var REPLAY_FROM_TIP = -1L
        private const val AUTHORIZATION = "Authorization"
        private val log = LoggerFactory.getLogger(EmpConnectorVariant::class.java)
        private fun topicWithoutQueryString(fullTopic: String): String {
            return fullTopic.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }
    }
}
