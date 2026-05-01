package xyz.candycrawler.draftsimparser.application.port

data class ArticleVectorDocument(
    val id: String,
    val content: String,
    val metadata: Map<String, Any>,
)

data class ArticleVectorSearchMatch(
    val articleId: Long,
    val score: Double?,
    val content: String,
    val metadata: Map<String, Any>,
)

interface ArticleVectorStore {
    fun replaceArticleDocuments(articleId: Long, documents: List<ArticleVectorDocument>)
    fun search(query: String, topK: Int, similarityThreshold: Double): List<ArticleVectorSearchMatch>
}
