package no.nav.crm.kafka.activity

import com.salesforce.emp.connector.EmpConnector
import no.nav.crm.kafka.activity.nais.enableNAISAPI

// Environment dependencies injected in pod by nais kafka solution
val EMP_URL = System.getenv("EMP_URL")
// val EMP_USERNAME = System.getenv("crm-kafka-activity.EMP_USERNAME")
// val EMP_PASSWORD = System.getenv("crm-kafka-activity.EMP_PASSWORD")
val EMP_TOPIC = System.getenv("EMP_TOPIC")

object Bootstrap {

    fun start() {
        enableNAISAPI {
            EMP.processEvents(
                EMP_URL,
                "EMP_USERNAME",
                "EMP_PASSWORD",
                "/topic/$EMP_TOPIC",
                EmpConnector.REPLAY_FROM_EARLIEST
            )
        }
    }
}
