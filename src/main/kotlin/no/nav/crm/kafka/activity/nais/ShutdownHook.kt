package no.nav.crm.kafka.activity.nais

import mu.KotlinLogging

object ShutdownHook {

    private val log = KotlinLogging.logger { }

    @Volatile
    private var shutdownhookActiveOrOther = false
    private val mainThread: Thread = Thread.currentThread()

    init {
        log.info { "Installing shutdown hook" }
        Runtime.getRuntime()
            .addShutdownHook(
                object : Thread() {
                    override fun run() {
                        shutdownhookActiveOrOther = true
                        log.info { "shutdown hook activated" }
                        mainThread.join()
                    }
                }
            )
    }

    fun isActive() = shutdownhookActiveOrOther
    fun reset() { shutdownhookActiveOrOther = false }
}
