package xyz.candycrawler.draftsimparser.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.model.ArticlePage
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository

@Service
class ArticleService(
    private val queryArticleRepository: QueryArticleRepository,
    private val articleRepository: ArticleRepository,
    private val articleKeywordService: ArticleKeywordService,
    private val articleSemanticSearchService: ArticleSemanticSearchService,
    private val articleAnalysisPublisher: ArticleAnalysisPublisher,
) {

    fun search(query: String?, page: Int, pageSize: Int, favoriteOnly: Boolean?): ArticlePage =
        queryArticleRepository.search(query, page, pageSize, favoriteOnly)

    fun semanticSearch(
        query: String,
        topK: Int?,
        similarityThreshold: Double?,
        favoriteOnly: Boolean?,
    ): List<ArticleSemanticSearchResult> =
        articleSemanticSearchService.search(query, topK, similarityThreshold, favoriteOnly)

    fun findById(id: Long): Article =
        queryArticleRepository.findById(id)

    fun updateFavorite(id: Long, favorite: Boolean): Article =
        articleRepository.update(id) { it.copy(favorite = favorite) }

    fun analyze(ids: List<Long>): List<Article> =
        ids.map { id ->
            val article = queryArticleRepository.findById(id)
            articleAnalysisPublisher.publish(id)
            article
        }

    fun collectKeywords(ids: List<Long>): List<Article> {
        val articles = ids.map { id -> queryArticleRepository.findById(id) }
        articleKeywordService.collectAsync(ids)
        return articles
    }

    fun findByIds(ids: List<Long>): List<Article> =
        ids.map { id -> queryArticleRepository.findById(id) }
}
