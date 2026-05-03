package xyz.candycrawler.wizardstataggregator.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsPage
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSearchCriteria
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository.CardLimitedStatsRepository

@Service
class CardLimitedStatsSearchService(private val repository: CardLimitedStatsRepository) {
    fun search(criteria: CardLimitedStatsSearchCriteria): CardLimitedStatsPage = repository.search(criteria)
}
