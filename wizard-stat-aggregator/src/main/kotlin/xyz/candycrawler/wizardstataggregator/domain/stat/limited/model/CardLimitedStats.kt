package xyz.candycrawler.wizardstataggregator.domain.stat.limited.model

import xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception.InvalidCardLimitedStatsException

data class CardLimitedStats(
    val id: Long? = null,
    val name: String,
    val mtgaId: Int,
    val setCode: String,
    val matchType: String,
    val tier: String? = null,
    val color: String,
    val rarity: String,
    val url: String,
    val urlBack: String,
    val types: List<String>,
    val layout: String,
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
) {
    init {
        fun invalid(reason: String): Nothing = throw InvalidCardLimitedStatsException(reason)

        if (name.isBlank()) invalid("name must not be blank")
        if (mtgaId <= 0) invalid("mtgaId must be positive")
        if (setCode.isBlank()) invalid("setCode must not be blank")
        if (matchType.isBlank()) invalid("matchType must not be blank")
        if (tier != null && tier.isBlank()) invalid("tier must not be blank")
        if (url.isBlank()) invalid("url must not be blank")
        if (types.isEmpty()) invalid("types must not be empty")
        if (seenCount < 0) invalid("seenCount must be non-negative")
        if (pickCount < 0) invalid("pickCount must be non-negative")
        if (gameCount < 0) invalid("gameCount must be non-negative")
        if (poolCount < 0) invalid("poolCount must be non-negative")
        if (openingHandGameCount < 0) invalid("openingHandGameCount must be non-negative")
        if (drawnGameCount < 0) invalid("drawnGameCount must be non-negative")
        if (everDrawnGameCount < 0) invalid("everDrawnGameCount must be non-negative")
        if (neverDrawnGameCount < 0) invalid("neverDrawnGameCount must be non-negative")
        if (playRate !in 0.0..1.0) invalid("playRate must be in [0, 1]")
        if (winRate !in 0.0..1.0) invalid("winRate must be in [0, 1]")
        if (openingHandWinRate !in 0.0..1.0) invalid("openingHandWinRate must be in [0, 1]")
        if (drawnWinRate !in 0.0..1.0) invalid("drawnWinRate must be in [0, 1]")
        if (everDrawnWinRate !in 0.0..1.0) invalid("everDrawnWinRate must be in [0, 1]")
        if (neverDrawnWinRate !in 0.0..1.0) invalid("neverDrawnWinRate must be in [0, 1]")
        if (avgSeen != null && avgSeen < 0.0) invalid("avgSeen must be non-negative")
        if (avgPick != null && avgPick < 0.0) invalid("avgPick must be non-negative")
    }
}
