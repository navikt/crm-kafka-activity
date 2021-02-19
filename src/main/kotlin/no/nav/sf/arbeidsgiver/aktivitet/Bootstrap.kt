package no.nav.sf.arbeidsgiver.aktivitet

import mu.KotlinLogging

import com.salesforce.emp.connector.example.DevLoginExample

/**
 * Bootstrap is a very simple µService manager
 * - start, enables mandatory NAIS API before entering 'work' loop
 * - loop,  invokes a work -, then a wait session, until shutdown - or prestop hook (NAIS feature),
 * - conditionalWait, waits a certain time, checking the hooks once a while
 */

object Bootstrap {

    private val log = KotlinLogging.logger { }

    fun start() {
        log.info { "Starting" }
        log.info { "Finished!" }

        DevLoginExample("https://test.salesforce.com", "john.foreland@nav.no.preprod", "n3M21ZGTixFKn3M21ZGTixFKC5JQjw1FHfGGEVWhcMBPmd18i");

    }
}
