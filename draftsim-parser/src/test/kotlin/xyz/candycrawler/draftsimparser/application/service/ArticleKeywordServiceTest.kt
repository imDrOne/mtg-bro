package xyz.candycrawler.draftsimparser.application.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ArticleKeywordServiceTest {

    private val queryArticleRepository = mock<QueryArticleRepository>()
    private val articleRepository = mock<ArticleRepository>()
    private val extractor = ArticleKeywordExtractor()
    private val service = ArticleKeywordService(queryArticleRepository, articleRepository, extractor)

    @Test
    fun `collect extracts and stores keywords`() {
        val article = article(textContent = "Removal spell wins games. Removal spell rewards control decks.")
        whenever(queryArticleRepository.findById(1)).thenReturn(article)
        whenever(articleRepository.update(eq(1), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(Article) -> Article>(1)
            block(article)
        }

        val updated = service.collect(1)

        assertEquals("removal spell", updated.keywords.first())
        verify(articleRepository).update(eq(1), any())
    }

    private fun article(textContent: String?) = Article(
        id = 1,
        externalId = 100,
        title = "Draft Guide",
        slug = "draft-guide",
        url = "https://draftsim.com/draft-guide",
        htmlContent = null,
        textContent = textContent,
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
