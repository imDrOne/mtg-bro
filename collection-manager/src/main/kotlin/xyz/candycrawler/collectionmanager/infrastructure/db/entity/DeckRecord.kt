package xyz.candycrawler.collectionmanager.infrastructure.db.entity

import java.time.LocalDateTime

data class DeckRecord(
    val id: Long?,
    val userId: Long,
    val name: String,
    val format: String,
    val colorIdentity: List<String>,
    val comment: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
