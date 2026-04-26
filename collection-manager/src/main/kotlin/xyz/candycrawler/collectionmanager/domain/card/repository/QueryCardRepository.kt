package xyz.candycrawler.collectionmanager.domain.card.repository

import xyz.candycrawler.collectionmanager.domain.card.model.Card

interface QueryCardRepository {
    fun findAllInCollection(userId: Long): List<Card>
}
