package xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest
import xyz.candycrawler.wizardstataggregator.lib.LocalOnly

@LocalOnly
class Lands17ApiClientTest @Autowired constructor(private val apiClient: Lands17ApiClient) : AbstractIntegrationTest() {

    @Test
    fun getStatistic() {
        val result = apiClient.getStatistic(
            setCode = "ECL",
            matchType = Lands17ApiClient.MatchType.QUICK_DRAFT.value,
        )
        logger.debug(result.toString())
    }
}
