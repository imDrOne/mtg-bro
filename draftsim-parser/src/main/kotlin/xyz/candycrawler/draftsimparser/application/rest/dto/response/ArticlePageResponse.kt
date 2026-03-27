package xyz.candycrawler.draftsimparser.application.rest.dto.response

import xyz.candycrawler.draftsimparser.domain.article.model.ArticlePage

data class ArticlePageResponse(
    val articles: List<ArticleResponse>,
    val totalArticles: Long,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
)

fun ArticlePage.toResponse() = ArticlePageResponse(
    articles = articles.map { it.toResponse() },
    totalArticles = totalArticles,
    page = page,
    pageSize = pageSize,
    hasMore = hasMore,
)
