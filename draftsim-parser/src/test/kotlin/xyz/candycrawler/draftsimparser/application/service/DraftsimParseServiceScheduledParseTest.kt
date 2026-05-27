package xyz.candycrawler.draftsimparser.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSearchResult
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSource
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTaskStatus
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import xyz.candycrawler.draftsimparser.domain.set.model.ActiveSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension::class)
class DraftsimParseServiceScheduledParseTest {

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
    fun `startScheduledParse returns null when no tags found for set`() {
        val set = activeSet("BLB", "Bloomburrow")
        whenever(articleSource.findTagIdsBySetName("Bloomburrow")).thenReturn(emptyList())

        val result = service.startScheduledParse(set)

        assertNull(result)
        verify(parseTaskRepository, never()).save(any())
    }

    @Test
    fun `startScheduledParse returns UUID and creates task with set keyword when tags found`() {
        val set = activeSet("BLB", "Bloomburrow")
        whenever(articleSource.findTagIdsBySetName("Bloomburrow")).thenReturn(listOf(42L, 99L))

        val taskId = UUID.randomUUID()
        val taskCaptor = argumentCaptor<ParseTask>()
        whenever(parseTaskRepository.save(taskCaptor.capture())).thenAnswer { invocation ->
            invocation.getArgument<ParseTask>(0).copy(id = taskId)
        }
        whenever(parseTaskRepository.update(any(), any<(ParseTask) -> ParseTask>())).thenAnswer { invocation ->
            val block = invocation.getArgument<(ParseTask) -> ParseTask>(1)
            block(pendingTask(taskId))
        }
        whenever(articleSource.searchArticlesByTagIds(any(), any(), any())).thenReturn(
            DraftsimArticleSearchResult(articles = emptyList(), totalPages = 1, totalArticles = 0),
        )

        val result = service.startScheduledParse(set)

        assertNotNull(result)
        assertEquals("set:BLB", taskCaptor.firstValue.keyword)
    }

    private fun activeSet(code: String, name: String) = ActiveSet(
        code = code,
        name = name,
        releasedAt = LocalDate.of(2024, 8, 2),
        setType = "expansion",
    )

    private fun pendingTask(id: UUID) = ParseTask(
        id = id,
        keyword = "set:BLB",
        status = ParseTaskStatus.PENDING,
        totalArticles = null,
        processedArticles = 0,
        errorMessage = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}
