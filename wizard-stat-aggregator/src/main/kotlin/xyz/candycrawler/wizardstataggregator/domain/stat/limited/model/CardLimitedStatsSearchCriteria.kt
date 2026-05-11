package xyz.candycrawler.wizardstataggregator.domain.stat.limited.model

data class CardLimitedStatsSearchCriteria(
    val setCode: String,
    val matchType: String,
    val names: List<String> = emptyList(),
    val mtgaIds: List<Int> = emptyList(),
    val tiers: List<String?> = emptyList(),
    val minWinRate: Double? = null,
    val maxWinRate: Double? = null,
    val order: CardLimitedStatsSortOrder = CardLimitedStatsSortOrder.WIN_RATE,
    val direction: CardLimitedStatsSortDirection = CardLimitedStatsSortDirection.DESC,
    val page: Int = 1,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    init {
        require(setCode.isNotBlank()) { "setCode must not be blank" }
        require(matchType.isNotBlank()) { "matchType must not be blank" }
        tiers.filterNotNull().forEach { require(it.isNotBlank()) { "tiers must not contain blank values" } }
        require(page >= 1) { "page must be >= 1" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "pageSize must be between 1 and $MAX_PAGE_SIZE" }
        minWinRate?.let { require(it in 0.0..1.0) { "minWinRate must be in [0, 1]" } }
        maxWinRate?.let { require(it in 0.0..1.0) { "maxWinRate must be in [0, 1]" } }
        if (minWinRate != null && maxWinRate != null) {
            require(minWinRate <= maxWinRate) { "minWinRate must be <= maxWinRate" }
        }
    }

    val offset: Long = ((page - 1) * pageSize).toLong()

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
    }
}

data class CardLimitedStatsPage(
    val stats: List<CardLimitedStats>,
    val totalStats: Long,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int,
)

enum class CardLimitedStatsSortOrder {
    NAME,
    MTGA_ID,
    WIN_RATE,
    GAME_COUNT,
    DRAWN_IMPROVEMENT_WIN_RATE,
    ;

    companion object {
        fun fromString(value: String?): CardLimitedStatsSortOrder = if (value == null) {
            WIN_RATE
        } else {
            entries.find { it.name.equals(value, ignoreCase = true) } ?: WIN_RATE
        }
    }
}

enum class CardLimitedStatsSortDirection {
    ASC,
    DESC,
    ;

    companion object {
        fun fromString(value: String?): CardLimitedStatsSortDirection = if (value == null) {
            DESC
        } else {
            entries.find { it.name.equals(value, ignoreCase = true) } ?: DESC
        }
    }
}
