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
val EMP_URL = System.getenv("EMP_URL")
val EMP_USERNAME = System.getenv("EMP_USERNAME")
val EMP_PASSWORD = System.getenv("EMP_PASSWORD")
val EMP_TOPIC = if (System.getenv("EMP_TOPIC")) "https://salesforce.com" ? else "https://test.salesforce.com"

private val log = KotlinLogging.logger { }

object Bootstrap {

    fun start() {
        enableNAISAPI {
            log.info { "Initiate event processing" }
            EMP.processEvents(
                EMP_URL,
                EMP_USERNAME,
                EMP_PASSWORD,
                "/topic/$EMP_TOPIC",
                EmpConnector.REPLAY_FROM_EARLIEST
            )
            log.info { "Initiated event processing successfully" }
            log.info { "Starting loop to continue running event processing in the background" }
            loop()
        }
    }

    // needs loop to tell kubernetes processEvents() is still running and it not finished nor failing
    private tailrec fun loop() {
        val stop = ShutdownHook.isActive() || PrestopHook.isActive()
        when {
            stop -> Unit
            !stop -> {
                // do not run start() again, as processEvents() processes events as they occurs (not in batches)
                conditionalWait(600000) // Suspended wait (should not hog resources)
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
