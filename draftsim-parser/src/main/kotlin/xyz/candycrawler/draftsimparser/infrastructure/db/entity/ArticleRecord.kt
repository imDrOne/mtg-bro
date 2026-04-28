package xyz.candycrawler.draftsimparser.infrastructure.db.entity

import java.time.LocalDateTime

data class ArticleRecord(
    val id: Long?,
    val externalId: Long,
    val title: String,
    val slug: String,
    val url: String,
    val htmlContent: String?,
    val textContent: String?,
    val analyzedText: String?,
    val keywords: List<String>,
    val favorite: Boolean,
    val errorMsg: String?,
    val analyzStartedAt: LocalDateTime?,
    val analyzEndedAt: LocalDateTime?,
    val publishedAt: LocalDateTime?,
    val fetchedAt: LocalDateTime?,
)
