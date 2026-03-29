package xyz.candycrawler.draftsimparser.application.rest.dto.response

import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.time.LocalDateTime

data class ArticleSummaryResponse(
    val id: Long,
    val externalId: Long,
    val title: String,
    val slug: String,
    val url: String,
    val analyzedText: String?,
    val favorite: Boolean,
    val errorMsg: String?,
    val analyzStartedAt: LocalDateTime?,
    val analyzEndedAt: LocalDateTime?,
    val publishedAt: LocalDateTime?,
    val fetchedAt: LocalDateTime?,
)

fun Article.toSummaryResponse() = ArticleSummaryResponse(
    id = id!!,
    externalId = externalId,
    title = title,
    slug = slug,
    url = url,
    analyzedText = analyzedText,
    favorite = favorite,
    errorMsg = errorMsg,
    analyzStartedAt = analyzStartedAt,
    analyzEndedAt = analyzEndedAt,
    publishedAt = publishedAt,
    fetchedAt = fetchedAt,
)
