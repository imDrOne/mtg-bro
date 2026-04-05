package xyz.candycrawler.collectionmanager.domain.deck.model

import xyz.candycrawler.collectionmanager.domain.deck.exception.InvalidDeckException

class DeckEntry(
    val id: Long? = null,
    val deckId: Long? = null,
    val cardId: Long,
    val quantity: Int,
    val isSideboard: Boolean = false,
) {
    init {
        if (cardId <= 0) throw InvalidDeckException("cardId must be positive")
        if (quantity <= 0) throw InvalidDeckException("quantity must be positive")
        if (quantity > 4) throw InvalidDeckException("quantity must not exceed 4 for any single card")
    }
}
