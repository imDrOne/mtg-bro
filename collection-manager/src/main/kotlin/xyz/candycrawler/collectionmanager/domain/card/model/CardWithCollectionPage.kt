package xyz.candycrawler.collectionmanager.domain.card.model

data class CardWithCollectionPage(
    val cards: List<CardWithCollection>,
    val totalCards: Long,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int,
)
