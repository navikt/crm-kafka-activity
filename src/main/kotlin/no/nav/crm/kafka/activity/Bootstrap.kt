package no.nav.crm.kafka.activity

import com.salesforce.emp.connector.EmpConnector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.crm.kafka.activity.nais.PrestopHook
import no.nav.crm.kafka.activity.nais.ShutdownHook
import no.nav.crm.kafka.activity.nais.enableNAISAPI

private val log = KotlinLogging.logger { }

object Bootstrap {

    fun start(env: SystemEnvironment) {
        enableNAISAPI {
            conditionalWait(2000)
            log.info { "Initiate event processing v.${env.VERSION}" }
            EMP.processEvents(
                env,
                EmpConnector.REPLAY_FROM_EARLIEST
            )
            log.info { "Initiated event processing successfully" }
            log.info { "Starting loop to continue running event processing in the background v.${env.VERSION}" }

            conditionalWait(env.EMP_CONNECTION_WAIT) // Allow 1 minute for EMP to connect

            log.info { "Will enter loop with connected state: ${EMP.connector.isConnected}" }
            loop(env)
        }
    }

    // needs loop to tell kubernetes processEvents() is still running and it not finished nor failing
    private tailrec fun loop(env: SystemEnvironment) {
        val stop = ShutdownHook.isActive() || PrestopHook.isActive() || EMP.connector.isDisconnected
        when {
            stop -> {
                log.info { "Stopping with states ShutdownHook ${ShutdownHook.isActive()}, PrestopHook ${PrestopHook.isActive()}, EMP disconnected ${EMP.connector.isDisconnected}" }
                conditionalWait(2000)
                Unit
            }
            !stop -> {
                env.workloopHook()
                // do not run start() again, as processEvents() processes events as they occurs (not in batches)
                conditionalWait(env.WORK_LOOP_WAIT) // Suspended wait 10 min (should not hog resources)
                loop(env)
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
