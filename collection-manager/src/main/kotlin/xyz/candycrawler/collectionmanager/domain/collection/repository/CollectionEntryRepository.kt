package xyz.candycrawler.collectionmanager.domain.collection.repository

import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry

interface CollectionEntryRepository {
    fun saveAll(entries: List<CollectionEntry>)
    fun findByCardId(cardId: Long): CollectionEntry?
    fun findAll(): List<CollectionEntry>
}
