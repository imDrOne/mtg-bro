package xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest
import xyz.candycrawler.wizardstataggregator.lib.LocalOnly

@LocalOnly
class Lands17ApiClientFacadeTest(@Autowired val lands17ApiClientFacade: Lands17ApiClientFacade) :
    AbstractIntegrationTest() {

    @Test
    fun getDraftStatistic() {
        val result = lands17ApiClientFacade.getDraftStatistic(setCode = "ECL")
        logger.debug(result.toString())
    }

    @Test
    fun getSealedStatistic() {
        val result = lands17ApiClientFacade.getSealedStatistic(setCode = "ECL")
        logger.debug(result.toString())
    }
}
