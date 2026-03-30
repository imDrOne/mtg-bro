package xyz.candycrawler.draftsimparser.application.rest.dto.response

import xyz.candycrawler.draftsimparser.domain.article.model.Article

data class ArticleAnalysisResponse(
    val id: Long,
    val title: String,
    val analyzedText: String?,
)

fun Article.toAnalysisResponse() = ArticleAnalysisResponse(
    id = id!!,
    title = title,
    analyzedText = analyzedText,
)
