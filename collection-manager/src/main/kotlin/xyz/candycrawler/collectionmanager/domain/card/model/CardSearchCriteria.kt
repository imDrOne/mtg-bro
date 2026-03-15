package xyz.candycrawler.collectionmanager.domain.card.model

data class CardSearchCriteria(
    val query: String? = null,
    val setCode: String? = null,
    val collectorNumber: String? = null,
    val colors: List<String>? = null,
    val colorIdentity: List<String>? = null,
    val type: String? = null,
    val rarity: String? = null,
    val order: CardSortOrder = CardSortOrder.NAME,
    val direction: SortDirection = SortDirection.AUTO,
    val page: Int = 1,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 175
    }
}

enum class CardSortOrder {
    NAME, SET, RELEASED, RARITY, COLOR, USD, EUR, CMC, POWER, TOUGHNESS, ARTIST;

    companion object {
        fun fromString(value: String?): CardSortOrder =
            if (value == null) NAME
            else entries.find { it.name.equals(value, ignoreCase = true) } ?: NAME
    }
}

enum class SortDirection {
    AUTO, ASC, DESC;

    companion object {
        fun fromString(value: String?): SortDirection =
            if (value == null) AUTO
            else entries.find { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}
