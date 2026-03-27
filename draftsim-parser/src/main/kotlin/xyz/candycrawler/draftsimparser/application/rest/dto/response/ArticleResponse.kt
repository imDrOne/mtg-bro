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
    publishedAt = publishedAt,
    fetchedAt = fetchedAt,
)
