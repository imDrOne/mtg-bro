package xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository

import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats

interface CardLimitedStatsRepository {
    fun saveAll(cardStats: List<CardLimitedStats>)
    fun findById(id: Long): CardLimitedStats
    fun findByMatchType(matchType: String): List<CardLimitedStats>
    fun findByMtgaIdAndMatchType(mtgaId: Int, setCode: String, matchType: String): CardLimitedStats?
    fun findByNameAndSetCodeAndMatchType(name: String, setCode: String, matchType: String): CardLimitedStats?
}
