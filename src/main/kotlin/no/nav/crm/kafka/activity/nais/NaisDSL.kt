package no.nav.crm.kafka.activity.nais

import java.io.StringWriter
import mu.KotlinLogging
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer

private val log = KotlinLogging.logger { }

const val NAIS_URL = "http://localhost:"
const val NAIS_DEFAULT_PORT = 8080

const val NAIS_ISALIVE = "/isAlive"
const val NAIS_ISREADY = "/isReady"
const val NAIS_METRICS = "/metrics"
const val NAIS_PRESTOP = "/stop"

/*
internal val preStopHook: Gauge = Gauge
        .build()
        .name("pre_stop__hook_gauge")
        .help("No. of preStopHook activations since last restart")
        .register()*/

private fun String.responseByContent(): Response =
    if (this.isNotEmpty()) Response(Status.OK).body(this) else Response(Status.NO_CONTENT)

fun naisAPI(): HttpHandler = routes(

    NAIS_ISALIVE bind Method.GET to { Response(Status.OK) },
    NAIS_ISREADY bind Method.GET to { Response(Status.OK) },
    NAIS_METRICS bind Method.GET to {
        runCatching {
            StringWriter().let { str ->
                // TextFormat.write004(str, cRegistry.metricFamilySamples())
                str
            }.toString()
        }
            .onFailure {
                log.error { "/prometheus failed writing metrics - ${it.localizedMessage}" }
            }
            .getOrDefault("")
            .responseByContent()
    },
    NAIS_PRESTOP bind Method.GET to {
        // preStopHook.inc()
        PrestopHook.activate()
        log.info { "Received PreStopHook from NAIS" }
        Response(Status.OK)
    }
)

fun naisAPIServer(port: Int): Http4kServer = naisAPI().asServer(Netty(port))

fun enableNAISAPI(port: Int = NAIS_DEFAULT_PORT, doSomething: () -> Unit): Boolean =
    naisAPIServer(port).let { srv ->
        try {
            srv.start().use {
                log.info { "NAIS DSL is up and running at port $port" }
                runCatching(doSomething)
                    .onFailure {
                        log.error { "Failure during doSomething in enableNAISAPI - ${it.localizedMessage}" }
                    }
            }
            true
        } catch (e: Exception) {
            log.error { "Failure during enable/disable NAIS api for port $port - ${e.localizedMessage}" }
            false
        } finally {
            srv.close()
            log.info { "NAIS DSL is stopped at port $port" }
        }
    }
