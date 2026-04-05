package xyz.candycrawler.collectionmanager.domain.deck.model

import java.time.LocalDateTime

data class DeckHeader(
    val id: Long,
    val name: String,
    val format: DeckFormat,
    val colorIdentity: List<String>,
    val comment: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
