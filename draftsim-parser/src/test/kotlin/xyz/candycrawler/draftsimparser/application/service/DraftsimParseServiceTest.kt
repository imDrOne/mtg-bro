package xyz.candycrawler.draftsimparser.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSource
import xyz.candycrawler.draftsimparser.application.port.DraftsimSourceArticle
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension::class)
class DraftsimParseServiceTest {

    private val parseTaskRepository = mock<ParseTaskRepository>()
    private val articleRepository = mock<ArticleRepository>()
    private val articleSource = mock<DraftsimArticleSource>()
    private val articleAnalysisPublisher = mock<ArticleAnalysisPublisher>()
    private val parseAlertService = mock<ParseAlertService>()
    private val keywordExtractor = ArticleKeywordExtractor()
    private val service = DraftsimParseService(
        parseTaskRepository = parseTaskRepository,
        articleRepository = articleRepository,
        articleSource = articleSource,
        articleAnalysisPublisher = articleAnalysisPublisher,
        parseAlertService = parseAlertService,
        articleKeywordExtractor = keywordExtractor,
        autoPublish = false,
    )

    @Test
    fun `processPost extracts and stores keywords during article parsing`() {
        var savedArticle: Article? = null
        whenever(articleRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Article>(0).copy(id = 1).also { savedArticle = it }
        }
        whenever(articleRepository.update(eq(1), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(Article) -> Article>(1)
            block(savedArticle!!)
        }
        val taskId = UUID.randomUUID()

        val result = invokeProcessArticle(taskId, sourceArticle())

        assertEquals("removal spell", result.keywords.first())
        assertTrue("draft" in result.keywords)
        verify(articleRepository).saveTaskArticleLink(taskId, 1)
        verify(articleRepository).update(eq(1), any())
    }

    private fun invokeProcessArticle(taskId: UUID, article: DraftsimSourceArticle): Article {
        val method = DraftsimParseService::class.java.getDeclaredMethod(
            "processArticle",
            UUID::class.java,
            DraftsimSourceArticle::class.java,
        )
        method.isAccessible = true
        return method.invoke(service, taskId, article) as Article
    }

    private fun sourceArticle() = DraftsimSourceArticle(
        externalId = 100,
        publishedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        slug = "draft-guide",
        url = "https://draftsim.com/draft-guide",
        title = "Draft Guide",
        htmlContent =
            """
            <p>The best removal spell is a removal spell.</p>
            <p>Removal wins draft games.</p>
            <p>Draft strategy rewards another removal spell.</p>
            """.trimIndent(),
        textContent = """
            The best removal spell is a removal spell.

            Removal wins draft games.

            Draft strategy rewards another removal spell.
        """.trimIndent(),
    )
}
