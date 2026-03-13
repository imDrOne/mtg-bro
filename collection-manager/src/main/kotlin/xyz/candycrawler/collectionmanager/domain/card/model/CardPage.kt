package xyz.candycrawler.collectionmanager.domain.card.model

data class CardPage(
    val cards: List<Card>,
    val totalCards: Long,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int,
)
