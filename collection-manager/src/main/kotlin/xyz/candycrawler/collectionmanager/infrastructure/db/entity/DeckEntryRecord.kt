package xyz.candycrawler.collectionmanager.infrastructure.db.entity

data class DeckEntryRecord(
    val id: Long?,
    val userId: Long,
    val deckId: Long,
    val cardId: Long,
    val quantity: Int,
    val isSideboard: Boolean,
)
