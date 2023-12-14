package com.salesforce.emp.connector.example

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.ByteBufferContentProvider
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.UnsupportedEncodingException
import java.net.ConnectException
import java.net.URL
import java.nio.ByteBuffer
import javax.xml.parsers.SAXParserFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * A helper to obtain the Authentication bearer token via login
 *
 * @author hal.hildebrand
 * modified by bjorn.hagglund
 */
object LoginHelperVariant {
    const val COMETD_REPLAY = "/cometd/"
    const val COMETD_REPLAY_OLD = "/cometd/replay/"
    const val LOGIN_ENDPOINT = "https://login.salesforce.com"
    private const val ENV_END = "</soapenv:Body></soapenv:Envelope>"
    private const val ENV_START = (
        "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' " +
            "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xmlns:urn='urn:partner.soap.sforce.com'><soapenv:Body>"
        )

    // The enterprise SOAP API endpoint used for the login call
    private const val soapUri = "/services/Soap/u/44.0/"
    private val defaultHttpClient = { s: SslContextFactory -> HttpClient(s) }

    @Throws(Exception::class)
    fun login(username: String, password: String): BayeuxParametersVariant {
        return login(URL(LOGIN_ENDPOINT), username, password, defaultHttpClient)
    }

    @Throws(Exception::class)
    fun login(username: String, password: String, params: BayeuxParametersVariant): BayeuxParametersVariant {
        return login(URL(LOGIN_ENDPOINT), username, password, defaultHttpClient, params)
    }

    @JvmOverloads
    @Throws(Exception::class)
    fun login(
        loginEndpoint: URL?,
        username: String,
        password: String,
        httpClientFn: (SslContextFactory) -> HttpClient,
        parameters: BayeuxParametersVariant = object : BayeuxParametersVariant {
            override fun bearerToken(): String {
                throw IllegalStateException("Have not authenticated")
            }

            override fun endpoint(): URL {
                throw IllegalStateException("Have not established replay endpoint")
            }
        }
    ): BayeuxParametersVariant {
        val client = httpClientFn(parameters.sslContextFactory())
        return try {
            client.proxyConfiguration.proxies.addAll(parameters.proxies())
            client.start()
            val endpoint = URL(loginEndpoint, soapUri)
            val post = client.POST(endpoint.toURI())
            post.content(ByteBufferContentProvider("text/xml", ByteBuffer.wrap(soapXmlForLogin(username, password))))
            post.header("SOAPAction", "''")
            post.header("PrettyPrint", "Yes")
            val response = post.send()
            val spf = SAXParserFactory.newInstance()
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false)
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            spf.isNamespaceAware = true
            val saxParser = spf.newSAXParser()
            val parser = LoginResponseParser()
            saxParser.parse(ByteArrayInputStream(response.content), parser)
            val sessionId = parser.sessionId
            if (sessionId == null || parser.serverUrl == null) {
                throw ConnectException(String.format("Unable to login: %s", parser.faultstring))
            }
            val soapEndpoint = URL(parser.serverUrl)
            val cometdEndpoint = if (parameters.version().toFloat() < 37) COMETD_REPLAY_OLD else COMETD_REPLAY
            val replayEndpoint = URL(
                soapEndpoint.protocol,
                soapEndpoint.host,
                soapEndpoint.port,
                StringBuilder().append(cometdEndpoint).append(parameters.version()).toString()
            )
            object : DelegatingBayeuxParametersVariant(parameters) {
                override fun bearerToken(): String {
                    return sessionId
                }

                override fun endpoint(): URL {
                    return replayEndpoint
                }
            }
        } finally {
            client.stop()
            client.destroy()
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun soapXmlForLogin(username: String, password: String): ByteArray {
        return (
            ENV_START + "  <urn:login>" + "    <urn:username>" + username + "</urn:username>" + "    <urn:password>" +
                password + "</urn:password>" + "  </urn:login>" + ENV_END
            ).toByteArray(charset("UTF-8"))
    }

    private class LoginResponseParser : DefaultHandler() {
        private var buffer: String? = null
        var faultstring: String? = null
        private var reading = false
        var serverUrl: String? = null
        var sessionId: String? = null
        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (reading) buffer = String(ch, start, length)
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            reading = false
            when (localName) {
                "sessionId" -> sessionId = buffer
                "serverUrl" -> serverUrl = buffer
                "faultstring" -> faultstring = buffer
                else -> {}
            }
            buffer = null
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            when (localName) {
                "sessionId" -> reading = true
                "serverUrl" -> reading = true
                "faultstring" -> reading = true
                else -> {}
            }
        }
    }
}
