package no.nav.crm.kafka.activity

fun main(env: SystemEnvironment) = Bootstrap.start(env)
fun main() {
    Bootstrap.start(SystemEnvironment())
}
