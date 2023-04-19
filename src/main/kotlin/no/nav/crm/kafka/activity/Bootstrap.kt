package no.nav.crm.kafka.activity

import com.salesforce.emp.connector.EmpConnector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.crm.kafka.activity.nais.PrestopHook
import no.nav.crm.kafka.activity.nais.ShutdownHook
import no.nav.crm.kafka.activity.nais.enableNAISAPI

// Environment dependencies injected in pod by nais kafka solution
val EMP_USERNAME = System.getenv("EMP_USERNAME")
val EMP_PASSWORD = System.getenv("EMP_PASSWORD")
val EMP_TOPIC = System.getenv("EMP_TOPIC")
val EMP_URL = if (System.getenv("EMP_ENV") == "prod") "https://navdialog.my.salesforce.com"
                else if (System.getenv("EMP_ENV") == "dev") "https://test.salesforce.com"
                else ""

val VERSION = System.getenv("VERSION")

private val log = KotlinLogging.logger { }

object Bootstrap {

    fun start() {
        enableNAISAPI {
            conditionalWait(2000)
            log.info { "Initiate event processing v.$VERSION" }
            EMP.processEvents(
                EMP_URL,
                EMP_USERNAME,
                EMP_PASSWORD,
                "/topic/$EMP_TOPIC",
                EmpConnector.REPLAY_FROM_EARLIEST
            )
            log.info { "Initiated event processing successfully" }
            log.info { "Starting loop to continue running event processing in the background v.$VERSION" }

            conditionalWait(60_000) // Allow 1 minute for EMP to connect

            log.info { "Will enter loop with connected state: ${EMP.connector.isConnected}" }
            loop()
        }
    }

    // needs loop to tell kubernetes processEvents() is still running and it not finished nor failing
    private tailrec fun loop() {
        val stop = ShutdownHook.isActive() || PrestopHook.isActive() || EMP.connector.isDisconnected
        when {
            stop -> {
                log.info { "Stopping with states ShutdownHook ${ShutdownHook.isActive()}, PrestopHook ${PrestopHook.isActive()}, EMP disconnected ${EMP.connector.isDisconnected}" }
                conditionalWait(2000)
                Unit
            }
            !stop -> {
                // do not run start() again, as processEvents() processes events as they occurs (not in batches)
                conditionalWait(600_000) // Suspended wait 10 min (should not hog resources)
                loop()
            }
        }
    }
}

fun conditionalWait(ms: Long) =
    runBlocking {
        val cr = launch {
            runCatching { delay(ms) }
        }

        tailrec suspend fun loop(): Unit = when {
            cr.isCompleted -> Unit
            ShutdownHook.isActive() || PrestopHook.isActive() -> cr.cancel()
            else -> {
                delay(250L)
                loop()
            }
        }
        loop()
        cr.join()
    }
