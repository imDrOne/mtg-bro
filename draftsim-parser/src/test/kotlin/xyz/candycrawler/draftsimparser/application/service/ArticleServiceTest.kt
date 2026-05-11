package xyz.candycrawler.draftsimparser.application.service

import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.domain.article.exception.ArticleNotFoundException
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleServiceTest {

    private val queryArticleRepository = mock<QueryArticleRepository>()
    private val articleRepository = mock<ArticleRepository>()
    private val articleKeywordService = mock<ArticleKeywordService>()
    private val articleSemanticSearchService = mock<ArticleSemanticSearchService>()
    private val articleAnalysisPublisher = mock<ArticleAnalysisPublisher>()
    private val articleVectorIndexService = mock<ArticleVectorIndexService>()
    private val service = ArticleService(
        queryArticleRepository = queryArticleRepository,
        articleRepository = articleRepository,
        articleKeywordService = articleKeywordService,
        articleSemanticSearchService = articleSemanticSearchService,
        articleAnalysisPublisher = articleAnalysisPublisher,
        articleVectorIndexService = articleVectorIndexService,
    )

    @Test
    fun `analyze loads articles and publishes analysis requests`() {
        val article = article(id = 1)
        whenever(queryArticleRepository.findById(1)).thenReturn(article)

        val result = service.analyze(listOf(1))

        assertEquals(listOf(article), result)
        verify(articleAnalysisPublisher).publish(1)
    }

    @Test
    fun `collectKeywords returns current articles and starts async keyword collection`() {
        val article = article(id = 1)
        whenever(queryArticleRepository.findById(1)).thenReturn(article)

        val result = service.collectKeywords(listOf(1))

        assertEquals(listOf(article), result)
        verify(articleKeywordService).collectAsync(eq(listOf(1)))
    }

    @Test
    fun `reindexVectors returns current articles and starts async vector indexing`() {
        val article = article(id = 1)
        whenever(queryArticleRepository.findById(1)).thenReturn(article)

        val result = service.reindexVectors(listOf(1))

        assertEquals(listOf(article), result)
        verify(articleVectorIndexService).replaceIndexesAsync(eq(listOf(article)))
        verify(articleSemanticSearchService).evictSearchCache()
    }

    @Test
    fun `findByIds returns all articles when all ids are found`() {
        val article1 = article(id = 1)
        val article2 = article(id = 2)
        whenever(queryArticleRepository.findById(1)).thenReturn(article1)
        whenever(queryArticleRepository.findById(2)).thenReturn(article2)

        val result = service.findByIds(listOf(1, 2))

        assertEquals(listOf(article1, article2), result)
    }

    @Test
    fun `findByIds skips missing ids and returns only found articles`() {
        val article1 = article(id = 1)
        whenever(queryArticleRepository.findById(1)).thenReturn(article1)
        whenever(queryArticleRepository.findById(99)).thenThrow(ArticleNotFoundException(99))

        val result = service.findByIds(listOf(1, 99))

        assertEquals(listOf(article1), result)
    }

    @Test
    fun `findByIds returns empty list when all ids are missing`() {
        whenever(queryArticleRepository.findById(10)).thenThrow(ArticleNotFoundException(10))
        whenever(queryArticleRepository.findById(20)).thenThrow(ArticleNotFoundException(20))

        val result = service.findByIds(listOf(10, 20))

        assertTrue(result.isEmpty())
    }

    private fun article(id: Long) = Article(
        id = id,
        externalId = 100,
        title = "Draft Guide",
        slug = "draft-guide",
        url = "https://draftsim.com/draft-guide",
        htmlContent = null,
        textContent = null,
        analyzedText = null,
        keywords = emptyList(),
        favorite = false,
        errorMsg = null,
        analyzStartedAt = null,
        analyzEndedAt = null,
        publishedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        fetchedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
    )
}
