package com.salesforce.emp.connector.example

import com.salesforce.emp.connector.LoginHelper
import org.eclipse.jetty.client.ProxyConfiguration
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * @author hal.hildebrand
 * modified by bjorn.hagglund
 */
interface BayeuxParametersVariant {
    /**
     * @return the bearer token used to authenticate
     */
    fun bearerToken(): String

    /**
     * @return the URL of the platform Streaming API endpoint
     */
    fun endpoint(): URL {
        val path = StringBuilder().append(LoginHelper.COMETD_REPLAY).append(version()).toString()
        return try {
            URL(host(), path)
        } catch (e: MalformedURLException) {
            throw IllegalArgumentException(
                String.format("Unable to create url: %s:%s", host().toExternalForm(), path),
                e
            )
        }
    }

    fun host(): URL {
        return try {
            URL(LoginHelperVariant.LOGIN_ENDPOINT)
        } catch (e: MalformedURLException) {
            throw IllegalStateException(String.format("Unable to form URL for %s", LoginHelperVariant.LOGIN_ENDPOINT))
        }
    }

    /**
     * @return the keep alive interval duration
     */
    fun keepAlive(): Long {
        return 30
    }

    /**
     * @return keep alive interval time unit
     */
    fun keepAliveUnit(): TimeUnit {
        return TimeUnit.MINUTES
    }

    fun longPollingOptions(): Map<String, Any> {
        val options: MutableMap<String, Any> = HashMap()
        options["maxNetworkDelay"] = maxNetworkDelay()
        options["maxBufferSize"] = maxBufferSize()
        return options
    }

    /**
     * @return The long polling transport maximum number of bytes of a HTTP response, which may contain many Bayeux
     * messages
     */
    fun maxBufferSize(): Int {
        return 1048576 / 2
    }

    /**
     * @return The long polling transport maximum number of milliseconds to wait before considering a request to the
     * Bayeux server failed
     */
    fun maxNetworkDelay(): Int {
        return 15000 * 2
    }

    /**
     * @return a list of proxies to use for outbound connections
     */
    fun proxies(): Collection<ProxyConfiguration.Proxy> {
        return emptyList<ProxyConfiguration.Proxy>()
    }

    /**
     * @return the SslContextFactory for establishing secure outbound connections
     */
    fun sslContextFactory(): SslContextFactory {
        return SslContextFactory()
    }

    /**
     * @return the Streaming API version
     */
    fun version(): String {
        return "57.0"
    }
}
