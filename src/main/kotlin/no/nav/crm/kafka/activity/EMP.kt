package no.nav.crm.kafka.activity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.salesforce.emp.connector.example.BayeuxParametersVariant
import com.salesforce.emp.connector.example.EmpConnectorVariant
import com.salesforce.emp.connector.example.LoginHelperVariant
import com.salesforce.emp.connector.example.BearerTokenProviderVariant
import com.salesforce.emp.connector.example.LoggingListener
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
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
    lateinit var connector: EmpConnectorVariant

    @Throws(Throwable::class)
    fun processEvents(env: SystemEnvironment, replayFrom: Long) {

        val supplier: BayeuxParametersVariant
        try {
            supplier = LoginHelperVariant.login(URL(env.EMP_URL), env.EMP_USERNAME, env.EMP_PASSWORD)
        } catch (e: Exception) {
            log.error { "Error at login: " + e.message.toString() }
            throw RuntimeException(e)
        }

        log.info { "Login Supplier done" }

        val tokenProvider = BearerTokenProviderVariant(Supplier { supplier })

        val params = tokenProvider.login()

        connector = EmpConnectorVariant(params)

        val logl = LoggingListener()
        val logListener = LogListener()
        connector.addListener(META_HANDSHAKE, logListener)
            .addListener(META_CONNECT, logListener)
            .addListener(META_DISCONNECT, logListener)
            .addListener(META_SUBSCRIBE, logListener)
            .addListener(META_UNSUBSCRIBE, logListener)

        connector.setBearerTokenProvider(tokenProvider)
        val result = connector.start()[30, TimeUnit.SECONDS]

        log.info { "Connection result : $result" }

        try {
            connector.subscribe("/topic/${env.EMP_TOPIC}", replayFrom, processData())[30, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            log.error { "Subscribe ExecutionException:" + e.message }
            throw e.cause!!
        } catch (e: TimeoutException) {
            log.error { "Subscribe TimeoutException:" + e.message }
            throw e.cause!!
        }
    }

    private val workerThreadPool = Executors.newFixedThreadPool(1)

    private var latestReplayId: String = "" // TODO Quick fix to stop double posting. Should investigate reason data seems to be handled twice from salesforce
    private var latestRecordId: String = ""

    fun processData(): Consumer<Map<String, Any>> {
        return Consumer<Map<String, Any>> { event ->

            workerThreadPool.submit {
                val eventObject = JSON.toString(event.get("event"))
                val eventMap = ObjectMapper().readValue<MutableMap<Any, String>>(eventObject)
                val replayId = eventMap.get("replayId")

                val sObject = JSON.toString(event.get("sobject"))
                val sObjectMap = ObjectMapper().readValue<MutableMap<Any, String>>(sObject)
                val recordId = sObjectMap.get("Id")

                if ((replayId == latestReplayId) && (recordId == latestRecordId)) {
                    log.info { "Duplicate ignored.\nReplay ID: $replayId.\nRecord ID: $recordId" }
                } else {
                    latestReplayId = replayId ?: ""
                    latestRecordId = recordId ?: ""

                    val producer = KafkaProducer<String, String>(kafkaProducerConfig)
                    val record = ProducerRecord(topic, replayId, JSON.toString(event))

                    producer.use { p ->
                        p.send(record) { m: RecordMetadata, e: Exception? ->
                            when (e) {
                                null -> {
                                    log.info { "Published to topic ${m.topic()}. partition [${m.partition()}] @ offset ${m.offset()} \nReplay ID: $replayId.\nRecord ID: $recordId" }
                                    Metrics.producedEvents.inc()
                                }
                                else -> log.error { "Failed to publish to topic ${m.topic()} \nReplay ID: $replayId. \nRecord ID: $recordId /n${e.cause}" }
                            }
                        }
                        p.flush()
                    }
                }
            }
        }
    }
}
