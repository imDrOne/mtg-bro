package xyz.candycrawler.draftsimparser.application.service

import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleServiceTest {

    private val queryArticleRepository = mock<QueryArticleRepository>()
    private val articleRepository = mock<ArticleRepository>()
    private val articleKeywordService = mock<ArticleKeywordService>()
    private val articleSemanticSearchService = mock<ArticleSemanticSearchService>()
    private val articleAnalysisPublisher = mock<ArticleAnalysisPublisher>()
    private val service = ArticleService(
        queryArticleRepository = queryArticleRepository,
        articleRepository = articleRepository,
        articleKeywordService = articleKeywordService,
        articleSemanticSearchService = articleSemanticSearchService,
        articleAnalysisPublisher = articleAnalysisPublisher,
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
