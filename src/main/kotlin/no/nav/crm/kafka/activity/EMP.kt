package no.nav.crm.kafka.activity

// import com.fasterxml.jackson.databind.ObjectMapper
// import com.fasterxml.jackson.module.kotlin.readValue
import com.salesforce.emp.connector.BayeuxParameters
import com.salesforce.emp.connector.EmpConnector
import com.salesforce.emp.connector.LoginHelper
import com.salesforce.emp.connector.example.BearerTokenProvider
import com.salesforce.emp.connector.example.LoggingListener
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
// import org.eclipse.jetty.util.ajax.JSON

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
        println("-------------------")
        println("url: $url")
        println("username: $username")
        println("topic: $topic")
        println("replayFrom: $replayFrom")
        println("-------------------")
        println("-------------------")

        val supplier: BayeuxParameters
        try {
            println("11")
            supplier = LoginHelper.login(URL(url), username, password)
        } catch (e: Exception) {
            println("error")
            println(e)

            throw RuntimeException(e)
        }

        println("1")

        val tokenProvider = BearerTokenProvider(Supplier { supplier })
        println("2")

        val params = tokenProvider.login()
        println("3")

        val connector = EmpConnector(params)
        println("4")

        val loggingListener = LoggingListener(false, false)
        connector.addListener(META_HANDSHAKE, loggingListener)
            .addListener(META_CONNECT, loggingListener)
            .addListener(META_DISCONNECT, loggingListener)
            .addListener(META_SUBSCRIBE, loggingListener)
            .addListener(META_UNSUBSCRIBE, loggingListener)

        println("5")

        connector.setBearerTokenProvider(tokenProvider)
        println("6")

        connector.start()[5, TimeUnit.SECONDS]
        println("7")

        try {
            connector.subscribe(topic, replayFrom, processData())[5, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            println("catch1")

            System.err.println(e.cause.toString())
            throw e.cause!!
        } catch (e: TimeoutException) {
            println("catch2")

            System.err.println("Timed out subscribing")
            throw e.cause!!
        }
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
