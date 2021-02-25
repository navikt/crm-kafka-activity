package no.nav.sf.arbeidsgiver.aktivitet

import com.salesforce.emp.connector.BayeuxParameters
import com.salesforce.emp.connector.EmpConnector
import com.salesforce.emp.connector.LoginHelper
import com.salesforce.emp.connector.TopicSubscription
import com.salesforce.emp.connector.example.BearerTokenProvider
import com.salesforce.emp.connector.example.LoggingListener
import org.cometd.bayeux.Channel.*
import java.lang.Exception
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.system.exitProcess

object EMP {

    @Throws(Throwable::class)
    fun processEvents(
        url: String,
        username: String,
        password: String,
        topic: String,
        replayFrom: Long,
        dataProcessMethod: Consumer<Map<String, Any>>
    ) {

        val supplier: BayeuxParameters;
        try {
            supplier = LoginHelper.login(URL(url), username, password)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        val tokenProvider = BearerTokenProvider(Supplier { supplier })
        val params = tokenProvider.login()
        val connector = EmpConnector(params)

        val loggingListener = LoggingListener(false, false)
        connector.addListener(META_HANDSHAKE, loggingListener)
            .addListener(META_CONNECT, loggingListener)
            .addListener(META_DISCONNECT, loggingListener)
            .addListener(META_SUBSCRIBE, loggingListener)
            .addListener(META_UNSUBSCRIBE, loggingListener)

        connector.setBearerTokenProvider(tokenProvider)
        connector.start()[5, TimeUnit.SECONDS]

        val subscription: TopicSubscription
        subscription = try {
            connector.subscribe(topic, replayFrom, dataProcessMethod)[5, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            System.err.println(e.cause.toString())
            throw e.cause!!
            exitProcess(1)
        } catch (e: TimeoutException) {
            System.err.println("Timed out subscribing")
            throw e.cause!!
            exitProcess(1)
        }
    }
}