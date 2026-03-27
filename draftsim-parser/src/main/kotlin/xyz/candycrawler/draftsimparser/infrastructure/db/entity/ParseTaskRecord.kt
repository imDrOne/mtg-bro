package xyz.candycrawler.draftsimparser.infrastructure.db.entity

import java.time.LocalDateTime
import java.util.UUID

data class ParseTaskRecord(
    val id: UUID?,
    val keyword: String,
    val status: String,
    val totalArticles: Int?,
    val processedArticles: Int,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
