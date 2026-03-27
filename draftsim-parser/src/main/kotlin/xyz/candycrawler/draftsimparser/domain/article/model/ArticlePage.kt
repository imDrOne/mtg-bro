package xyz.candycrawler.draftsimparser.domain.article.model

data class ArticlePage(
    val articles: List<Article>,
    val totalArticles: Long,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
)
