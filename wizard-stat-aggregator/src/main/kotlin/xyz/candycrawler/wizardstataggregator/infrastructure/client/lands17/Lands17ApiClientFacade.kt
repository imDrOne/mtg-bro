package xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17

import org.springframework.stereotype.Service
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.dto.response.CardStatsResponse

@Service
class Lands17ApiClientFacade(val lands17ApiClient: Lands17ApiClient) {

    fun getDraftStatistic(setCode: String): List<CardStatsResponse> = lands17ApiClient.getStatistic(
        setCode = setCode,
        matchType = Lands17ApiClient.MatchType.QUICK_DRAFT.value,
    )

    fun getSealedStatistic(setCode: String): List<CardStatsResponse> = lands17ApiClient.getStatistic(
        setCode = setCode,
        matchType = Lands17ApiClient.MatchType.SEALED.value,
    )
}
