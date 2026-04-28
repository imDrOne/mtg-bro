package xyz.candycrawler.draftsimparser.application.rest.dto.response

import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.time.LocalDateTime

data class ArticleResponse(
    val id: Long,
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

fun Article.toResponse() = ArticleResponse(
    id = id!!,
    externalId = externalId,
    title = title,
    slug = slug,
    url = url,
    htmlContent = htmlContent,
    textContent = textContent,
    analyzedText = analyzedText,
    keywords = keywords,
    favorite = favorite,
    errorMsg = errorMsg,
    analyzStartedAt = analyzStartedAt,
    analyzEndedAt = analyzEndedAt,
    publishedAt = publishedAt,
    fetchedAt = fetchedAt,
)
