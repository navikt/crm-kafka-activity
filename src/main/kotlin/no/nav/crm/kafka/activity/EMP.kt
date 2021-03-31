package no.nav.crm.kafka.activity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.salesforce.emp.connector.BayeuxParameters
import com.salesforce.emp.connector.EmpConnector
import com.salesforce.emp.connector.LoginHelper
import com.salesforce.emp.connector.example.BearerTokenProvider
import com.salesforce.emp.connector.example.LoggingListener
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.lang.Exception
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.function.Supplier
import org.cometd.bayeux.Channel.META_CONNECT
import org.cometd.bayeux.Channel.META_DISCONNECT
import org.cometd.bayeux.Channel.META_HANDSHAKE
import org.cometd.bayeux.Channel.META_SUBSCRIBE
import org.cometd.bayeux.Channel.META_UNSUBSCRIBE
import org.eclipse.jetty.util.ajax.JSON
import java.util.concurrent.Executors

private val log = KotlinLogging.logger { }

object EMP {

    @Throws(Throwable::class)
    fun processEvents(
        url: String,
        username: String,
        password: String,
        topic: String,
        replayFrom: Long
    ) {

        val supplier: BayeuxParameters
        try {
            supplier = LoginHelper.login(URL(url), username, password)
        } catch (e: Exception) {
            log.error { e.cause.toString() }
            throw RuntimeException(e)
        }

        val tokenProvider = BearerTokenProvider(Supplier { supplier })

        val params = tokenProvider.login()

        val connector = EmpConnector(params)

        val loggingListener = LoggingListener(false, true)
        connector.addListener(META_HANDSHAKE, loggingListener)
            .addListener(META_CONNECT, loggingListener)
            .addListener(META_DISCONNECT, loggingListener)
            .addListener(META_SUBSCRIBE, loggingListener)
            .addListener(META_UNSUBSCRIBE, loggingListener)

        connector.setBearerTokenProvider(tokenProvider)
        connector.start()[5, TimeUnit.SECONDS]

        try {
            connector.subscribe(topic, replayFrom, processData())[5, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            log.error { e.cause.toString() }
            throw e.cause!!
        } catch (e: TimeoutException) {
            log.error { e.cause.toString() }
            throw e.cause!!
        }
    }

    private val workerThreadPool = Executors.newFixedThreadPool(1)

    fun processData(): Consumer<Map<String, Any>> {
        return Consumer<Map<String, Any>> { event ->

            workerThreadPool.submit {
                val eventKey = JSON.toString(event.get("event"))
                val sobject = JSON.toString(event.get("sobject"))

                val map = ObjectMapper().readValue<MutableMap<Any, String>>(eventKey)
                val replayId = map.get("replayId")

                val producer = KafkaProducer<String, String>(kafkaProducerConfig)
                producer.use { p ->
                    p.send(ProducerRecord(topic, replayId, sobject))
                    p.flush()
                }
            }
        }
    }
}
