package xyz.candycrawler.draftsimparser.application.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorSearchMatch
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorStore
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.Duration
import java.util.Locale

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
    @Value("\${infrastructure.vector-index.search-cache.max-size}") searchCacheMaxSize: Long,
    @Value("\${infrastructure.vector-index.search-cache.ttl}") searchCacheTtl: Duration,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val searchCache: Cache<SearchCacheKey, List<ArticleSemanticSearchResult>> = Caffeine.newBuilder()
        .maximumSize(searchCacheMaxSize)
        .expireAfterWrite(searchCacheTtl)
        .build()

    fun search(
        query: String,
        topK: Int? = null,
        similarityThreshold: Double? = null,
        favoriteOnly: Boolean? = true,
    ): List<ArticleSemanticSearchResult> {
        val normalizedQuery = query.normalizeSemanticQuery()
        val vectorStore = vectorStoreProvider.getIfAvailable()

        return if (!enabled || normalizedQuery.isBlank() || vectorStore == null) {
            emptyList()
        } else {
            val effectiveTopK = topK?.coerceIn(1, MAX_TOP_K) ?: defaultTopK
            val effectiveSimilarityThreshold = similarityThreshold ?: defaultSimilarityThreshold
            val cacheKey = SearchCacheKey(
                query = normalizedQuery,
                topK = effectiveTopK,
                similarityThreshold = effectiveSimilarityThreshold,
                favoriteOnly = favoriteOnly,
            )

            searchCache.getIfPresent(cacheKey) ?: run {
                val matches = runCatching {
                    vectorStore.search(
                        query = normalizedQuery,
                        topK = effectiveTopK,
                        similarityThreshold = effectiveSimilarityThreshold,
                    )
                }.onFailure { ex ->
                    log.warn("Semantic article search failed for query='{}'", normalizedQuery, ex)
                }.getOrDefault(emptyList())

                matches
                    .groupBy { it.articleId }
                    .mapNotNull { (articleId, articleMatches) ->
                        val article = runCatching { queryArticleRepository.findById(articleId) }.getOrNull()
                        if (article == null || (favoriteOnly == true && !article.favorite)) {
                            null
                        } else {
                            ArticleSemanticSearchResult(
                                article = article,
                                score = articleMatches.mapNotNull { it.score }.maxOrNull(),
                                matches = articleMatches.map { it.toSemanticMatch() },
                            )
                        }
                    }
                    .sortedByDescending { it.score ?: 0.0 }
                    .also { searchCache.put(cacheKey, it) }
            }
        }
    }

    fun evictSearchCache() {
        searchCache.invalidateAll()
    }

    companion object {
        private const val MAX_TOP_K = 25
    }
}

private data class SearchCacheKey(
    val query: String,
    val topK: Int,
    val similarityThreshold: Double,
    val favoriteOnly: Boolean?,
)

private fun String.normalizeSemanticQuery(): String = trim()
    .lowercase(Locale.US)
    .replace(Regex("\\s+"), " ")

private fun ArticleVectorSearchMatch.toSemanticMatch(): ArticleSemanticSearchMatch = ArticleSemanticSearchMatch(
    score = score,
    content = content,
    insightType = metadata["insight_type"] as? String,
    subject = metadata["subject"] as? String,
    tags = metadata["tags"].toStringList(),
)

private fun Any?.toStringList(): List<String> = when (this) {
    is List<*> -> this.mapNotNull { it as? String }
    is Array<*> -> this.mapNotNull { it as? String }
    is String -> listOf(this)
    else -> emptyList()
}
