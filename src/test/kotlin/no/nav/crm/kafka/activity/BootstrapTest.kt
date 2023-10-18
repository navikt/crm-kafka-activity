package no.nav.crm.kafka.activity

import java.util.UUID
import kotlin.test.assertFalse
import no.nav.crm.kafka.activity.nais.PrestopHook
import org.apache.kafka.clients.producer.MockProducer
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.common.HashMapMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BootstrapTest {
    private lateinit var env: StubSystemEnvironment
    private lateinit var salesforcEventListener: ClientSessionChannel.MessageListener
    private lateinit var clientSessionChannel: ClientSessionChannel
    private lateinit var kafkaProducer: MockProducer<*, *>
    private var workloopEntered = false
    private lateinit var salesforceEvents: MutableList<HashMapMessage>

    @BeforeEach
    fun setUp() {
        kafkaProducer = TestProducer<String, String>()
        salesforceEvents = mutableListOf()

        // Most of the testing is done by capturing the callback function used for emitting Salesforce events.
        // This is done in the StubBayeuxClient where we use the salesforceEventListener() function
        // to hand back the callback function to the instance variable: salesforcEventListener here.

        // We also need to know when we have entered the work loop, this in order to know when
        // we should trigger events from Salesforce in the unit tests
        // We also supply a kafka producer, so we can verify that the events from Salesforce
        // where posted to a kafka channel.
        env = StubSystemEnvironment(::salesforceEventListener, ::workloopHook, kafkaProducer)
    }

    fun workloopHook() {
        workloopEntered = true
        triggerSalesforceEvents()
        shutdownWorkloop()
    }

    @AfterEach
    fun tearDown() {
        PrestopHook.reset()
        workloopEntered = false
    }

    // The communication with Salesforce is done in three stages
    // -- Login (fetch authentication-token and streaming api url)
    // -- Start connection ( do a handshake to prepare for subscription)
    // -- Subscribe
    @Test
    fun `login to Salesforce should enter workloop if Salesforce did issue authentication token`() {
        // arrange
        setupAuthenticationToken(token = "my_authentication_token")
        // act
        main(env)
        // assert
        assertTrue(workloopEntered)
    }

    @Test
    fun `login to Salesforce should not enter workloop if Salesforce did not issue authentication token`() {
        // arrange
        setupAuthenticationToken(token = null)
        // act
        main(env)
        // assert
        assertFalse(workloopEntered)
    }

    @Test
    fun `login to Salesforce should close the http client used to login`() {
        // act
        main(env)
        // assert
        assertTrue(env.wasHttpCLientStopIssued)
    }

    @Test
    fun `Salesforce handshake that fails should not enter workloop`() {
        // arrange
        env.isHandshakeSuccess = false
        // act
        main(env)
        // assert
        assertFalse(workloopEntered)
    }

    @Test
    fun `Salesforce handshake that is OK should enter workloop`() {
        // arrange
        env.isHandshakeSuccess = true
        // act
        main(env)
        // assert
        assertTrue(workloopEntered)
    }

    @Test
    fun `subscribe to Salesforce streaming API should not enter workloop if unable to start http client used to connect`() {
        // arrange
        env.isHttpClientStartError = true
        // act
        main(env)
        // assert
        assertFalse(workloopEntered)
    }

    @Test
    fun `subscribing to Salesforce events should not enter workloop if subscribe action fails`() {
        // arrange
        env.isSubscribeSuccess = false
        // act
        main(env)
        // assert
        assertFalse(workloopEntered)
    }

    @Test
    fun `Salesforce streaming API-url fetched when logging in to Salesforce should be used when subscribing to Salesforce events`() {
        // arrange
        val url = "http://salesforce.streaming.url"
        env.streamingURL = url
        setupSalesforceEvents(randomId())
        // act
        main(env)
        // assert
        assertTrue(streamingClient().url.contains(url))
    }

    @Test
    fun `Salesforce event should trigger message on Kafka stream`() {
        // arrange
        val id = randomId()
        setupSalesforceEvents(id)
        // act
        main(env)
        // assert
        assertTrue(kafkaProducer.history().size == 1)
        assertTrue((kafkaProducer.history().first().value() as CharSequence).contains(""""replayId":"$id""""))
    }

    @Test
    fun `Salesforce emitting two identical events should trigger just one message on Kafka stream`() {
        // arrange
        val id = randomId()
        setupSalesforceEvents(id, id)
        // act
        main(env)
        // assert
        assertTrue(kafkaProducer.history().size == 1)
    }

    @Test
    fun `Salesforce emitting three events should trigger two messages on Kafka stream`() {
        // arrange
        val id = randomId()
        val anotherId = randomId()
        setupSalesforceEvents(id, id, anotherId)
        // act
        main(env)
        // assert
        assertTrue(kafkaProducer.history().size == 2)
    }

    private fun salesforceEventListener(channel: ClientSessionChannel, listener: ClientSessionChannel.MessageListener) {
        this.clientSessionChannel = channel
        this.salesforcEventListener = listener
    }

    private fun triggerSalesforceEvents() =
        salesforceEvents.forEach { message ->
            salesforcEventListener.onMessage(clientSessionChannel, message)
        }

    private fun newMessage(replayId: String = "1") =
        HashMapMessage().apply {
            this[Message.DATA_FIELD] = mapOf(
                "event" to mapOf("replayId" to replayId),
                "sobject" to mapOf("Id" to replayId),
                "payload" to mapOf("key" to "value")
            )
        }

    private fun setupAuthenticationToken(token: String?) {
        env.loginSessionId = token
    }

    private fun setupSalesforceEvents(vararg id: String) =
        id.forEach { salesforceEvents.add(newMessage(replayId = it)) }

    private fun streamingClient() = env.bayeuxClient
    private fun shutdownWorkloop() = PrestopHook.activate()
    private fun randomId() = UUID.randomUUID().toString()

    inner class TestProducer<K, V> : MockProducer<K, V>() {
        override fun close() {}
    }
}
