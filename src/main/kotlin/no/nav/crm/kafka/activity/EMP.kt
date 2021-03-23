package no.nav.crm.kafka.activity

object EMP {

    @Throws(Throwable::class)
    fun processEvents(
        url: String,
        username: String,
        password: String,
        topic: String,
        replayFrom: Long
    ) {

        println("-------------------")
        println("username: $username")
    }
}
