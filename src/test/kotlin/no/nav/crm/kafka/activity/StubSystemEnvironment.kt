package no.nav.crm.kafka.activity

import java.lang.Exception
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.ClientTransport
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.HttpContentResponse
import org.eclipse.jetty.client.HttpConversation
import org.eclipse.jetty.client.HttpRequest
import org.eclipse.jetty.client.HttpResponse
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.api.Response
import org.eclipse.jetty.util.ssl.SslContextFactory

class StubSystemEnvironment(
    var eventListenerFn: (ClientSessionChannel, ClientSessionChannel.MessageListener) -> Unit,
    var workloopHookFn: (() -> Unit),
    val producer: Producer<*, *>
) : SystemEnvironment() {
    // Internal variables uses for testing
    var loginSessionId: String? = "1"
    var isHandshakeSuccess = true
    var isSubscribeSuccess = true
    var isHttpClientStartError = false
    var wasHttpCLientStopIssued = false
    var isSecondStart = false
    var streamingURL = "http://streaming.url"
    lateinit var bayeuxClient: BayeuxClient

    override val EMP_CONNECTION_WAIT: Long = 10
    override val WORK_LOOP_WAIT: Long = 1000

    override val EMP_USERNAME = "EMP_USERNAME"
    override val EMP_PASSWORD = "EMP_PASSWORD"
    override val EMP_TOPIC = "EMP_TOPIC"
    override val EMP_URL = "http://login.to.salesforce.url"
    override val VERSION = "VERSION"

    override fun httpClient(contextFactory: SslContextFactory) =
        StubHttpClient(contextFactory).apply { connectTimeout = 90000000L }

    override fun bayeuxClient(url: String, transport: ClientTransport) =
        StubBayeuxClient(
            url,
            transport,
            isHandshakeSuccess,
            isSubscribeSuccess
        )
            .apply {
                this.testCallbackFn = eventListenerFn
                this@StubSystemEnvironment.bayeuxClient = this
            }

    override fun workloopHook() = workloopHookFn() // Used to transfer control back to the test
    override fun kafkaConfigs() = Configurations({ param -> param })

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> kafkaProducer(): Producer<K, V> = producer as MockProducer<K, V>

    /** We intercept the ExecutorService to enforce that the work issued from
     * the work loop is executed before the test is finished. */
    override fun workerThreadPool(): ExecutorService {
        return object : ExecutorService {
            override fun execute(command: Runnable) {
                TODO("Not yet implemented")
            }

            override fun shutdown() {
                TODO("Not yet implemented")
            }

            override fun shutdownNow(): MutableList<Runnable> {
                TODO("Not yet implemented")
            }

            override fun isShutdown(): Boolean {
                TODO("Not yet implemented")
            }

            override fun isTerminated(): Boolean {
                TODO("Not yet implemented")
            }

            override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
                TODO("Not yet implemented")
            }

            override fun <Any> submit(task: Callable<Any>): Future<Any> {
                TODO("Not yet implemented")
            }

            override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
                TODO("Not yet implemented")
            }

            override fun submit(task: Runnable): Future<*> {
                task.run()
                return CompletableFuture<Any>().also { it.complete("" as Any) }
            }

            override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
                TODO("Not yet implemented")
            }

            override fun <T : Any?> invokeAll(
                tasks: MutableCollection<out Callable<T>>,
                timeout: Long,
                unit: TimeUnit
            ): MutableList<Future<T>> {
                TODO("Not yet implemented")
            }

            override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
                TODO("Not yet implemented")
            }

            override fun <T : Any?> invokeAny(
                tasks: MutableCollection<out Callable<T>>,
                timeout: Long,
                unit: TimeUnit
            ): T {
                TODO("Not yet implemented")
            }
        }
    }

    /** A stub class used to intercept the http-calls to Salesforce **/
    inner class StubHttpClient(contextFactory: SslContextFactory) : HttpClient(contextFactory) {
        override fun POST(uri: URI): Request {
            return StubRequest(uri)
        }

        override fun doStart() {
            if (isHttpClientStartError && isSecondStart) throw Exception()
            isSecondStart = true
            super.start()
        }

        override fun doStop() {
            wasHttpCLientStopIssued = true
            super.doStop()
        }
    }

    inner class StubRequest(uri: URI) :
        HttpRequest(StubHttpClient(SslContextFactory.Client()), HttpConversation(), uri) {
        override fun send() =
            HttpContentResponse(
                HttpResponse(this, emptyList<Response.ResponseListener>()),
                soapResponse(),
                "application/octet-stream",
                "UTF-8"
            )

        fun soapResponse() =
            """<?xml version="1.0"?>
        <soap:Envelope
        	xmlns:soap="http://www.w3.org/2003/05/soap-envelope/"
            soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
	        <soap:Body>
	            ${sessionIdTag()}
	            ${serverUrlTag()}
	            <faultstring>faultstring</faultstring>
	        </soap:Body>
        </soap:Envelope>  
            """.toByteArray()

        fun sessionIdTag() = loginSessionId?.run { "<sessionId>$this</sessionId>" } ?: ""
        fun serverUrlTag() = "<serverUrl>$streamingURL</serverUrl>"
    }
}
