package no.nav.crm.kafka.activity

import java.util.Properties
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

// App environment dependencies
const val EV_kafkaTopic = "KAFKA_TOPIC"
const val EV_kafkaClientID = "KAFKA_CLIENTID"

// Environment dependencies injected in pod by nais kafka solution
const val EV_kafkaBrokers = "KAFKA_BROKERS"
const val EV_kafkaKeystorePath = "KAFKA_KEYSTORE_PATH"
const val EV_kafkaCredstorePassword = "KAFKA_CREDSTORE_PASSWORD"
const val EV_kafkaTruststorePath = "KAFKA_TRUSTSTORE_PATH"

class Configurations(private val envFn: (String) -> String = System::getenv) {
    val topic = fetchEnv(EV_kafkaTopic)

    val kafkaProducerConfig: Properties = mapOf<String, Any>(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to fetchEnv(EV_kafkaBrokers),
        ProducerConfig.CLIENT_ID_CONFIG to fetchEnv(EV_kafkaClientID),
        ProducerConfig.ACKS_CONFIG to "all",
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to 60000,
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to fetchEnv(EV_kafkaKeystorePath),
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to fetchEnv(EV_kafkaCredstorePassword),
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to fetchEnv(EV_kafkaTruststorePath),
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to fetchEnv(EV_kafkaCredstorePassword)
    )
        .asProperties()

    val kafkaConsumerConfig: Properties = mapOf<String, Any>(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to fetchEnv(EV_kafkaBrokers),
        ConsumerConfig.GROUP_ID_CONFIG to fetchEnv(EV_kafkaClientID),
        ConsumerConfig.CLIENT_ID_CONFIG to fetchEnv(EV_kafkaClientID),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 200, // Match maximum when sending to SF REST API
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false", // Do not commit until operation with data is confirmed
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to fetchEnv(EV_kafkaKeystorePath),
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to fetchEnv(EV_kafkaCredstorePassword),
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to fetchEnv(EV_kafkaTruststorePath),
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to fetchEnv(EV_kafkaCredstorePassword)
    )
        .asProperties()

    fun Map<String, Any>.asProperties(): Properties {
        val props = Properties()
        this.forEach { (k, v) -> props[k] = v }
        return props
    }

    fun fetchEnv(env: String): String {
        return envFn(env)
    }
}
