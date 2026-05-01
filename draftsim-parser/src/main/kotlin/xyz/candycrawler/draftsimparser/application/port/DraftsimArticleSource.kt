package xyz.candycrawler.draftsimparser.application.port

import java.time.LocalDateTime

data class DraftsimSourceArticle(
    val externalId: Long,
    val title: String,
    val slug: String,
    val url: String,
    val htmlContent: String?,
    val textContent: String?,
    val publishedAt: LocalDateTime?,
)

data class DraftsimArticleSearchResult(
    val articles: List<DraftsimSourceArticle>,
    val totalPages: Int,
    val totalArticles: Int,
)

interface DraftsimArticleSource {
    fun searchArticles(keyword: String, page: Int, pageSize: Int = 10): DraftsimArticleSearchResult
}
