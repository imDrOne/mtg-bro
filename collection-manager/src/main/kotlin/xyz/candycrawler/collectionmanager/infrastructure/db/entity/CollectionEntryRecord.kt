package xyz.candycrawler.collectionmanager.infrastructure.db.entity

import java.time.LocalDateTime

data class CollectionEntryRecord(
    val id: Long?,
    val cardId: Long,
    val quantity: Int,
    val foil: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
