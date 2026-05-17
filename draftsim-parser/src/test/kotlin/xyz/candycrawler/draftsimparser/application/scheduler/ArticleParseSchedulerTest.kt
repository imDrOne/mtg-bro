package xyz.candycrawler.draftsimparser.application.scheduler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.application.port.ActiveSetSource
import xyz.candycrawler.draftsimparser.application.service.DraftsimParseService
import xyz.candycrawler.draftsimparser.application.service.ParseAlertService
import xyz.candycrawler.draftsimparser.domain.set.model.ActiveSet
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension::class)
class ArticleParseSchedulerTest {

    private val activeSetSource = mock<ActiveSetSource>()
    private val draftsimParseService = mock<DraftsimParseService>()
    private val parseAlertService = mock<ParseAlertService>()
    private val scheduler = ArticleParseScheduler(
        activeSetSource = activeSetSource,
        draftsimParseService = draftsimParseService,
        parseAlertService = parseAlertService,
    )

    @Test
    fun `run does nothing when no active sets returned`() {
        whenever(activeSetSource.fetchActiveSets()).thenReturn(emptyList())

        scheduler.run()

        verify(draftsimParseService, never()).startScheduledParse(any())
        verify(parseAlertService, never()).schedulerRunStarted(any())
    }

    @Test
    fun `run calls startScheduledParse for each set and emits scheduler alert`() {
        val setA = activeSet("BLB", "Bloomburrow")
        val setB = activeSet("DSK", "Duskmourn")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(setA, setB))

        scheduler.run()

        verify(draftsimParseService, times(1)).startScheduledParse(setA)
        verify(draftsimParseService, times(1)).startScheduledParse(setB)

        val codesCaptor = argumentCaptor<List<String>>()
        verify(parseAlertService).schedulerRunStarted(codesCaptor.capture())
        assertEquals(listOf("BLB", "DSK"), codesCaptor.firstValue)
    }

    @Test
    fun `run continues processing remaining sets when one throws`() {
        val setA = activeSet("BLB", "Bloomburrow")
        val setB = activeSet("DSK", "Duskmourn")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(setA, setB))
        whenever(draftsimParseService.startScheduledParse(setA)).thenThrow(RuntimeException("network error"))

        scheduler.run()

        verify(draftsimParseService).startScheduledParse(setA)
        verify(draftsimParseService).startScheduledParse(setB)
    }

    private fun activeSet(code: String, name: String) = ActiveSet(
        code = code,
        name = name,
        releasedAt = LocalDate.of(2024, 8, 2),
        setType = "expansion",
    )
}
