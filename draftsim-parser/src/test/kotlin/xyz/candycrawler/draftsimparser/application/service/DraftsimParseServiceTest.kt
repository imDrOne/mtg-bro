package xyz.candycrawler.draftsimparser.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.DraftsimWpApiClient
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto.WpPostResponse
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension::class)
class DraftsimParseServiceTest {

    private val parseTaskRepository = mock<ParseTaskRepository>()
    private val articleRepository = mock<ArticleRepository>()
    private val wpApiClient = mock<DraftsimWpApiClient>()
    private val eventPublisher = mock<ApplicationEventPublisher>()
    private val parseAlertService = mock<ParseAlertService>()
    private val keywordExtractor = ArticleKeywordExtractor()
    private val service = DraftsimParseService(
        parseTaskRepository = parseTaskRepository,
        articleRepository = articleRepository,
        wpApiClient = wpApiClient,
        eventPublisher = eventPublisher,
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

        val result = invokeProcessPost(taskId, post())

        assertEquals("removal spell", result.keywords.first())
        assertTrue("draft" in result.keywords)
        verify(articleRepository).saveTaskArticleLink(taskId, 1)
        verify(articleRepository).update(eq(1), any())
    }

    private fun invokeProcessPost(taskId: UUID, post: WpPostResponse): Article {
        val method = DraftsimParseService::class.java.getDeclaredMethod(
            "processPost",
            UUID::class.java,
            WpPostResponse::class.java,
        )
        method.isAccessible = true
        return method.invoke(service, taskId, post) as Article
    }

    private fun post() = WpPostResponse(
        id = 100,
        date = LocalDateTime.parse("2026-01-01T00:00:00"),
        slug = "draft-guide",
        link = "https://draftsim.com/draft-guide",
        title = WpPostResponse.WpRendered("Draft Guide"),
        content = WpPostResponse.WpRendered(
            """
            <p>The best removal spell is a removal spell.</p>
            <p>Removal wins draft games.</p>
            <p>Draft strategy rewards another removal spell.</p>
            """.trimIndent()
        ),
    )
}
