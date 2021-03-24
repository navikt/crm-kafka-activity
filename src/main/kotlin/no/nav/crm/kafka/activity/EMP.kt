package no.nav.crm.kafka.activity

import mu.KotlinLogging
// import com.fasterxml.jackson.databind.ObjectMapper
// import com.fasterxml.jackson.module.kotlin.readValue
import java.util.function.Consumer
// import org.eclipse.jetty.util.ajax.JSON

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

        println("-------------------")
        println("username: $username")

    }

    fun processData(): Consumer<Map<String, Any>> {
        return Consumer<Map<String, Any>> { event ->
            /*
            val eventKey = JSON.toString(event.get("event"))
            val sobject = JSON.toString(event.get("sobject"))

            val map = ObjectMapper().readValue<MutableMap<Any, String>>(eventKey)
            val replayId = map.get("replayId")
*/
            // val producer = KafkaProducer<String, String>(kafkaProducerConfig)
            // producer.use { p ->
            // p.send(ProducerRecord(topic, replayId, sobject))
            // p.flush()
            // }
        }
    }
}
