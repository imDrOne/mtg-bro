package xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository

import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsPage
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSearchCriteria

interface CardLimitedStatsRepository {
    fun saveAll(cardStats: List<CardLimitedStats>)
    fun findById(id: Long): CardLimitedStats
    fun findByMatchType(matchType: String): List<CardLimitedStats>
    fun findByMtgaIdAndMatchType(mtgaId: Int, setCode: String, matchType: String): CardLimitedStats?
    fun search(criteria: CardLimitedStatsSearchCriteria): CardLimitedStatsPage
}
