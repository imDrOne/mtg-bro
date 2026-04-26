package xyz.candycrawler.collectionmanager.domain.collection.repository

import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry

interface CollectionEntryRepository {
    fun saveAll(entries: List<CollectionEntry>)
    fun findByUserAndCardId(userId: Long, cardId: Long): List<CollectionEntry>
    fun findByUserAndCardIds(userId: Long, cardIds: List<Long>): List<CollectionEntry>
    fun findByUser(userId: Long): List<CollectionEntry>
}
