package no.nav.crm.kafka.activity

import mu.KotlinLogging
import mu.withLoggingContext
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSessionChannel

private val log = KotlinLogging.logger { }

class LogListener : ClientSessionChannel.MessageListener {
    override fun onMessage(channel: ClientSessionChannel, message: Message) {
        withLoggingContext(mapOf("success" to message.isSuccessful.toString(), "id" to message.id, "clientId" to message.clientId, "channel" to message.channel)) {
            if (message.isSuccessful) {
                // if (message.channel == "")
                log.info { message }
            } else {
                log.warn { message }
            }
        }
    }
}
