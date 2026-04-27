package xyz.candycrawler.collectionmanager.domain.card.model

data class CardWithCollection(
    val card: Card,
    val quantityNonFoil: Int,
    val quantityFoil: Int,
)
