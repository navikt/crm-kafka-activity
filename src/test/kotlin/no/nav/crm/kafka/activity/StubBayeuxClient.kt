package no.nav.crm.kafka.activity

import java.util.concurrent.CopyOnWriteArrayList
import org.cometd.bayeux.Channel
import org.cometd.bayeux.ChannelId
import org.cometd.bayeux.Message.Mutable
import org.cometd.bayeux.Message.SUCCESSFUL_FIELD
import org.cometd.bayeux.client.ClientSession
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.ClientTransport

open class StubBayeuxClient(
    url: String,
    transport: ClientTransport,
    private val isHandshakeSuccess: Boolean,
    private val isSubscribeSuccess: Boolean
) :
    BayeuxClient(url, transport) {

    lateinit var testCallbackFn: (ClientSessionChannel, ClientSessionChannel.MessageListener) -> Unit

    // The ClientChannel is where the http-connection to Salesforce is happening.
    // By stubbing this connection we can get hold of the callback function used for emitting Salesforce events.
    // We then hand back the callback function the test-framework, so that the tests can
    // use the callback to trigger Salesforce event.
    inner class StubBayeuxClientChannel(val id: ChannelId?) : ClientSessionChannel {
        private val _listeners = CopyOnWriteArrayList<ClientSessionChannel.ClientSessionChannelListener>()

        override fun getId() = "1"

        override fun getChannelId(): ChannelId {
            TODO("Not yet implemented")
        }

        override fun isMeta(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isService(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isBroadcast(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isWild(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isDeepWild(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setAttribute(name: String?, value: Any?) {
            TODO("Not yet implemented")
        }

        override fun getAttribute(name: String?): Any {
            TODO("Not yet implemented")
        }

        override fun getAttributeNames(): MutableSet<String> {
            TODO("Not yet implemented")
        }

        override fun removeAttribute(name: String?): Any {
            TODO("Not yet implemented")
        }

        override fun addListener(listener: ClientSessionChannel.ClientSessionChannelListener?) {}

        override fun removeListener(listener: ClientSessionChannel.ClientSessionChannelListener?) {
            TODO("Not yet implemented")
        }

        override fun getListeners(): MutableList<ClientSessionChannel.ClientSessionChannelListener> {
            TODO("Not yet implemented")
        }

        override fun getSession(): ClientSession {
            TODO("Not yet implemented")
        }

        override fun publish(data: Any?, callback: ClientSession.MessageListener?) {
            TODO("Not yet implemented")
        }

        override fun publish(message: Mutable?, callback: ClientSession.MessageListener?) {
            TODO("Not yet implemented")
        }

        override fun subscribe(
            message: Mutable?,
            listener: ClientSessionChannel.MessageListener,
            callback: ClientSession.MessageListener
        ): Boolean {
            // Here we hand the callback function; listening for Salesforce event, back to the tests
            testCallbackFn(this, listener)
            callback.onMessage(message(isSubscribeSuccess))
            return isSubscribeSuccess
        }

        override fun unsubscribe(
            message: Mutable?,
            listener: ClientSessionChannel.MessageListener?,
            callback: ClientSession.MessageListener?
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun unsubscribe() {
            TODO("Not yet implemented")
        }

        override fun getSubscribers(): MutableList<ClientSessionChannel.MessageListener> {
            TODO("Not yet implemented")
        }

        override fun release(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isReleased(): Boolean {
            TODO("Not yet implemented")
        }
    }

    override fun getChannel(channelId: ChannelId) =
        StubBayeuxClientChannel(channelId)

    override fun handshake(callback: ClientSessionChannel.MessageListener) {
        // Short-circuiting the Handshake process
        callback.onMessage(
            getChannel(Channel.META_HANDSHAKE),
            message(isHandshakeSuccess)
        )
    }

    override fun isDisconnected() = false // Just faking the connection
    private fun message(isSuccess: Boolean) = newMessage().apply { this[SUCCESSFUL_FIELD] = isSuccess }
}
