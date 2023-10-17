package no.nav.crm.kafka.activity

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.ClientTransport
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory

class SystemEnvironment {
    // Environment dependencies injected in pod by nais kafka solution
    open val EMP_USERNAME = System.getenv("EMP_USERNAME")
    open val EMP_PASSWORD = System.getenv("EMP_PASSWORD")
    open val EMP_TOPIC = System.getenv("EMP_TOPIC")
    open val EMP_URL =
        if (System.getenv("EMP_ENV") == "prod") "https://navdialog.my.salesforce.com"
        else if (System.getenv("EMP_ENV") == "dev") "https://test.salesforce.com"
        else ""

    open val VERSION = System.getenv("VERSION")
    open val WORK_LOOP_WAIT: Long = 600_000
    open val EMP_CONNECTION_WAIT: Long = 60_000

    open fun httpClient(contextFactory: SslContextFactory) = HttpClient(contextFactory)
    open fun bayeuxClient(url: String, transport: ClientTransport) = BayeuxClient(url, transport)
    open fun kafkaConfigs() = Configurations()
    open fun <K, V> kafkaProducer(): Producer<K, V> = KafkaProducer<K, V>(kafkaConfigs().kafkaProducerConfig)
}
