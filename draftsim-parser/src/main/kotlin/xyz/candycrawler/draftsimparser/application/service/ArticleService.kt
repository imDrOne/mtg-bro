package xyz.candycrawler.draftsimparser.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.model.ArticlePage
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSearchFilter
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSortField
import xyz.candycrawler.draftsimparser.domain.article.model.SortDirection
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDate

@Service
class ArticleService(
    private val queryArticleRepository: QueryArticleRepository,
    private val articleRepository: ArticleRepository,
    private val articleKeywordService: ArticleKeywordService,
    private val articleSemanticSearchService: ArticleSemanticSearchService,
    private val articleAnalysisPublisher: ArticleAnalysisPublisher,
    private val articleVectorIndexService: ArticleVectorIndexService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun search(
        query: String?,
        page: Int,
        pageSize: Int,
        favoriteOnly: Boolean?,
        setCode: String? = null,
        publishedFrom: LocalDate? = null,
        publishedTo: LocalDate? = null,
        sortBy: ArticleSortField = ArticleSortField.PUBLISHED_AT,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ArticlePage = queryArticleRepository.search(
        filter = ArticleSearchFilter(
            query = query,
            favoriteOnly = favoriteOnly,
            setCode = setCode,
            publishedFrom = publishedFrom,
            publishedTo = publishedTo,
            sortBy = sortBy,
            sortDirection = sortDirection,
        ),
        page = page,
        pageSize = pageSize,
    )

    fun semanticSearch(
        query: String,
        topK: Int?,
        similarityThreshold: Double?,
        favoriteOnly: Boolean?,
    ): List<ArticleSemanticSearchResult> =
        articleSemanticSearchService.search(query, topK, similarityThreshold, favoriteOnly)

    fun findById(id: Long): Article = queryArticleRepository.findById(id)

    fun updateFavorite(id: Long, favorite: Boolean): Article =
        articleRepository.update(id) { it.copy(favorite = favorite) }
            .also { articleSemanticSearchService.evictSearchCache() }

    fun analyze(ids: List<Long>): List<Article> = ids.map { id ->
        val article = queryArticleRepository.findById(id)
        articleAnalysisPublisher.publish(id)
        article
    }

    fun collectKeywords(ids: List<Long>): List<Article> {
        val articles = ids.map { id -> queryArticleRepository.findById(id) }
        articleKeywordService.collectAsync(ids)
        return articles
    }

    fun reindexVectors(ids: List<Long>): List<Article> {
        val articles = ids.map { id -> queryArticleRepository.findById(id) }
        articleVectorIndexService.replaceIndexesAsync(articles)
        articleSemanticSearchService.evictSearchCache()
        return articles
    }

    fun findByIds(ids: List<Long>): List<Article> = ids.mapNotNull { id ->
        runCatching { queryArticleRepository.findById(id) }
            .onFailure { log.warn("findByIds: id={} not found, skipping", id) }
            .getOrNull()
    }
}
