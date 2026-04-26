package xyz.candycrawler.collectionmanager.domain.deck.repository

import xyz.candycrawler.collectionmanager.domain.deck.model.Deck

interface DeckRepository {
    fun save(deck: Deck): Deck
    fun findByIdAndUser(id: Long, userId: Long): Deck
}
