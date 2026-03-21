package xyz.candycrawler.wizardstataggregator.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception.CardLimitedStatsNotFoundException
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.MatchType
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository.CardLimitedStatsRepository

@Service
class CardLimitedStatsQueryService(
    private val repository: CardLimitedStatsRepository,
) {

    fun getByCard(name: String, setCode: String, matchType: MatchType): CardLimitedStats =
        repository.findByNameAndSetCodeAndMatchType(name, setCode, matchType.value)
            ?: throw CardLimitedStatsNotFoundException(
                "name=$name, setCode=$setCode, matchType=${matchType.value}"
            )
}
