package xyz.candycrawler.draftsimparser.application.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorSearchMatch
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorStore
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository

data class ArticleSemanticSearchResult(
    val article: Article,
    val score: Double?,
    val matches: List<ArticleSemanticSearchMatch>,
)

data class ArticleSemanticSearchMatch(
    val score: Double?,
    val content: String,
    val insightType: String?,
    val subject: String?,
    val tags: List<String>,
)

@Service
class ArticleSemanticSearchService(
    private val vectorStoreProvider: ObjectProvider<ArticleVectorStore>,
    private val queryArticleRepository: QueryArticleRepository,
    @Value("\${infrastructure.vector-index.enabled}") private val enabled: Boolean,
    @Value("\${infrastructure.vector-index.top-k}") private val defaultTopK: Int,
    @Value("\${infrastructure.vector-index.similarity-threshold}") private val defaultSimilarityThreshold: Double,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun search(
        query: String,
        topK: Int? = null,
        similarityThreshold: Double? = null,
        favoriteOnly: Boolean? = true,
    ): List<ArticleSemanticSearchResult> {
        if (!enabled || query.isBlank()) return emptyList()
        val vectorStore = vectorStoreProvider.getIfAvailable() ?: return emptyList()

        val matches = runCatching {
            vectorStore.search(
                query = query,
                topK = topK?.coerceIn(1, MAX_TOP_K) ?: defaultTopK,
                similarityThreshold = similarityThreshold ?: defaultSimilarityThreshold,
            )
        }.onFailure { ex ->
            log.warn("Semantic article search failed for query='{}'", query, ex)
        }.getOrDefault(emptyList())

        return matches
            .groupBy { it.articleId }
            .mapNotNull { (articleId, articleMatches) ->
                val article = runCatching { queryArticleRepository.findById(articleId) }.getOrNull()
                    ?: return@mapNotNull null
                if (favoriteOnly == true && !article.favorite) return@mapNotNull null

                ArticleSemanticSearchResult(
                    article = article,
                    score = articleMatches.mapNotNull { it.score }.maxOrNull(),
                    matches = articleMatches.map { it.toSemanticMatch() },
                )
            }
            .sortedByDescending { it.score ?: 0.0 }
    }

    companion object {
        private const val MAX_TOP_K = 25
    }
}

private fun ArticleVectorSearchMatch.toSemanticMatch(): ArticleSemanticSearchMatch =
    ArticleSemanticSearchMatch(
        score = score,
        content = content,
        insightType = metadata["insight_type"] as? String,
        subject = metadata["subject"] as? String,
        tags = metadata["tags"].toStringList(),
    )

private fun Any?.toStringList(): List<String> =
    when (this) {
        is List<*> -> this.mapNotNull { it as? String }
        is Array<*> -> this.mapNotNull { it as? String }
        is String -> listOf(this)
        else -> emptyList()
    }
