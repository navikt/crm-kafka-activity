package no.nav.crm.kafka.activity

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging

object Metrics {

    private val log = KotlinLogging.logger { }

    val cRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

    val producedEvents: Gauge = Gauge
        .build()
        .name("produced_events")
        .help("produced_events") // The help text is not used but required
        .register()

    init {
        DefaultExports.initialize()
        log.info { "Prometheus metrics are ready" }
    }
}
