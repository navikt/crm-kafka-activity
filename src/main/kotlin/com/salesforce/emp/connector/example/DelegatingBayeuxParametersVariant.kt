package com.salesforce.emp.connector.example

import org.eclipse.jetty.client.ProxyConfiguration
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * @author hal.hildebrand
 * modified by bjorn.hagglund
 * @since API v37.0
 */
open class DelegatingBayeuxParametersVariant(private val parameters: BayeuxParametersVariant) :
    BayeuxParametersVariant {
    override fun bearerToken(): String {
        return parameters.bearerToken()
    }

    override fun endpoint(): URL {
        return parameters.endpoint()
    }

    override fun keepAlive(): Long {
        return parameters.keepAlive()
    }

    override fun keepAliveUnit(): TimeUnit {
        return parameters.keepAliveUnit()
    }

    override fun longPollingOptions(): Map<String, Any> {
        return parameters.longPollingOptions()
    }

    override fun maxBufferSize(): Int {
        return parameters.maxBufferSize()
    }

    override fun maxNetworkDelay(): Int {
        return parameters.maxNetworkDelay()
    }

    override fun proxies(): Collection<ProxyConfiguration.Proxy> {
        return parameters.proxies()
    }

    override fun sslContextFactory(): SslContextFactory {
        return parameters.sslContextFactory()
    }

    override fun version(): String {
        return parameters.version()
    }
}
