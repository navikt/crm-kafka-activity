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
val EMP_TOPIC = System.getenv("EMP_TOPIC")

private val log = KotlinLogging.logger { }

object Bootstrap {

    fun start() {
        enableNAISAPI {
            log.info { "Before EMP processEvents" }
            EMP.processEvents(
                EMP_URL,
                EMP_USERNAME,
                EMP_PASSWORD,
                "/topic/$EMP_TOPIC",
                EmpConnector.REPLAY_FROM_EARLIEST
            )
            log.info { "After EMP processEvents" }
            loop()
        }
    }

    private tailrec fun loop() {
        val stop = ShutdownHook.isActive() || PrestopHook.isActive()
        when {
            stop -> Unit
            !stop -> {
                conditionalWait(600000) // Suspended wait (should not hog resources)
                loop()
            }
        }
    }
}


fun conditionalWait(ms: Long) =
    runBlocking {
        log.info { "Will wait $ms ms" }
        val cr = launch {
            runCatching { delay(ms) }
                .onSuccess { log.info { "waiting completed" } }
                .onFailure { log.info { "waiting interrupted" } }
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
