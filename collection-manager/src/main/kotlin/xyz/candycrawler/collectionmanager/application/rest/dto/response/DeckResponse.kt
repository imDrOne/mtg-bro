package xyz.candycrawler.collectionmanager.application.rest.dto.response

import java.time.LocalDateTime

data class DeckListResponse(
    val decks: List<DeckHeaderResponse>,
)

data class DeckHeaderResponse(
    val id: Long,
    val name: String,
    val format: String,
    val colorIdentity: List<String>,
    val comment: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

data class DeckDetailResponse(
    val id: Long,
    val name: String,
    val format: String,
    val colorIdentity: List<String>,
    val comment: String?,
    val mainboard: List<DeckEntryResponse>,
    val sideboard: List<DeckEntryResponse>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

data class DeckEntryResponse(
    val cardId: Long,
    val quantity: Int,
)
