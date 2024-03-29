package no.nav.crm.kafka.activity.nais

import io.prometheus.client.exporter.common.TextFormat
import java.io.StringWriter
import mu.KotlinLogging
import no.nav.crm.kafka.activity.Metrics.cRegistry
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.ApacheServer
import org.http4k.server.Http4kServer
import org.http4k.server.asServer

private val log = KotlinLogging.logger { }

const val NAIS_URL = "http://localhost:"
const val NAIS_DEFAULT_PORT = 8080

const val NAIS_ISALIVE = "/isAlive"
const val NAIS_ISREADY = "/isReady"
const val NAIS_METRICS = "/metrics"
const val NAIS_PRESTOP = "/stop"

private fun String.responseByContent(): Response =
    if (this.isNotEmpty()) Response(Status.OK).body(this) else Response(Status.NO_CONTENT)

fun naisAPI(): HttpHandler = routes(

    NAIS_ISALIVE bind Method.GET to { Response(Status.OK) },
    NAIS_ISREADY bind Method.GET to { Response(Status.OK) },
    NAIS_METRICS bind Method.GET to {
        runCatching {
            StringWriter().let { str ->
                TextFormat.write004(str, cRegistry.metricFamilySamples())
                str
            }.toString()
        }
            .onFailure {
                log.error { "/prometheus failed writing metrics - ${it.message}" }
            }
            .getOrDefault("")
            .responseByContent()
    },
    NAIS_PRESTOP bind Method.GET to {
        PrestopHook.activate()
        log.info { "Received PreStopHook from NAIS" }
        Response(Status.OK)
    }
)

fun naisAPIServer(port: Int): Http4kServer = naisAPI().asServer(ApacheServer(port))

fun enableNAISAPI(port: Int = NAIS_DEFAULT_PORT, doSomething: () -> Unit): Boolean =
    naisAPIServer(port).let { srv ->
        try {
            srv.start().use {
                log.info { "NAIS DSL is up and running at port $port" }
                runCatching(doSomething)
                    .onFailure {
                        log.error { "Failure inside enableNAISAPI - ${it.message}" }
                    }
            }
            true
        } catch (e: Exception) {
            log.error { "Failure during enable/disable NAIS api for port $port - ${e.message}" }
            false
        } finally {
            srv.close()
            log.info { "NAIS DSL is stopped at port $port" }
        }
    }
