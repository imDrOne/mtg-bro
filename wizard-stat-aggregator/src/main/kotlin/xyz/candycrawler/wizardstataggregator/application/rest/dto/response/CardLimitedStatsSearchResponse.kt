package xyz.candycrawler.wizardstataggregator.application.rest.dto.response

import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsPage

data class CardLimitedStatsSearchResponse(
    val totalStats: Long,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int,
    val data: List<CardLimitedStatsResponse>,
)

data class CardLimitedStatsResponse(
    val name: String,
    val mtgaId: Int,
    val setCode: String,
    val matchType: String,
    val color: String,
    val rarity: String,
    val types: List<String>,
    val seenCount: Int,
    val avgSeen: Double?,
    val pickCount: Int,
    val avgPick: Double?,
    val gameCount: Int,
    val poolCount: Int,
    val playRate: Double,
    val winRate: Double,
    val openingHandGameCount: Int,
    val openingHandWinRate: Double,
    val drawnGameCount: Int,
    val drawnWinRate: Double,
    val everDrawnGameCount: Int,
    val everDrawnWinRate: Double,
    val neverDrawnGameCount: Int,
    val neverDrawnWinRate: Double,
    val drawnImprovementWinRate: Double,
)

fun CardLimitedStatsPage.toResponse(): CardLimitedStatsSearchResponse = CardLimitedStatsSearchResponse(
    totalStats = totalStats,
    hasMore = hasMore,
    page = page,
    pageSize = pageSize,
    data = stats.map { it.toResponse() },
)

private fun CardLimitedStats.toResponse(): CardLimitedStatsResponse = CardLimitedStatsResponse(
    name = name,
    mtgaId = mtgaId,
    setCode = setCode,
    matchType = matchType,
    color = color,
    rarity = rarity,
    types = types,
    seenCount = seenCount,
    avgSeen = avgSeen,
    pickCount = pickCount,
    avgPick = avgPick,
    gameCount = gameCount,
    poolCount = poolCount,
    playRate = playRate,
    winRate = winRate,
    openingHandGameCount = openingHandGameCount,
    openingHandWinRate = openingHandWinRate,
    drawnGameCount = drawnGameCount,
    drawnWinRate = drawnWinRate,
    everDrawnGameCount = everDrawnGameCount,
    everDrawnWinRate = everDrawnWinRate,
    neverDrawnGameCount = neverDrawnGameCount,
    neverDrawnWinRate = neverDrawnWinRate,
    drawnImprovementWinRate = drawnImprovementWinRate,
)
