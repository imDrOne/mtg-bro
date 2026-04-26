package xyz.candycrawler.collectionmanager.domain.deck.repository

import xyz.candycrawler.collectionmanager.domain.deck.model.DeckHeader

interface QueryDeckRepository {
    fun findHeaders(userId: Long): List<DeckHeader>
}
