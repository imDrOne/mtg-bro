package xyz.candycrawler.wizardstataggregator

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest
import kotlin.test.assertEquals

class WizardStatAggregatorApplicationTests(private val context: ApplicationContext) : AbstractIntegrationTest() {

    @Test
    fun contextLoads() {
        assertEquals(context.id, "wizard-stat-aggregator")
    }
}
