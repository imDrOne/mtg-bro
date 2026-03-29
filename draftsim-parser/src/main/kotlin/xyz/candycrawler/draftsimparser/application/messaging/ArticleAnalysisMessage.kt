package xyz.candycrawler.draftsimparser.application.messaging

data class ArticleAnalysisMessage(
    val articleId: Long,
    val paragraphs: List<String>,
    val slug: String,
    val url: String,
)
