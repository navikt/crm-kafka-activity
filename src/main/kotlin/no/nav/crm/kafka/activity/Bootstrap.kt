package no.nav.crm.kafka.activity

import com.salesforce.emp.connector.EmpConnector
import java.util.function.Consumer
import mu.KotlinLogging
import no.nav.crm.kafka.activity.nais.enableNAISAPI
import org.apache.kafka.clients.producer.KafkaProducer
import org.eclipse.jetty.util.ajax.JSON

// Environment dependencies injected in pod by nais kafka solution
const val EMP_URL = "KAFKA_URL"
const val EMP_USERNAME = "KAFKA_USERNAME"
const val EMP_PASSWORD = "KAFKA_PASSWORD"
const val EMP_TOPIC = "KAFKA_TOPIC"
const val EMP_REPLAYFROM = "KAFKA_REPLAYFROM"

private val log = KotlinLogging.logger { }

object Bootstrap {

    fun start() {
        enableNAISAPI {
            EMP.processEvents(
                    EMP_URL,
                    EMP_USERNAME,
                    EMP_PASSWORD,
                    EMP_TOPIC,
                    if (EMP_REPLAYFROM == "-1") EmpConnector.REPLAY_FROM_TIP else EmpConnector.REPLAY_FROM_EARLIEST,
                    processData()
            )
        }
    }

    val producer = KafkaProducer<String, String>(kafkaProducerConfig)

    fun processData(): Consumer<Map<String, Any>> {
        return Consumer<Map<String, Any>> { event ->
            println(JSON.toString(event.get("sobject")))
            val data = event.get("sobject")
            log.info { "Hej hopp" }
            producer.use { p ->
                // p.send(data)
                p.flush()
            }
        }
    }
}
