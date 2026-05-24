package xyz.candycrawler.draftsimparser.application.service

import com.github.benmanes.caffeine.cache.Ticker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.application.port.ActiveSetSource
import xyz.candycrawler.draftsimparser.configuration.ArticleParseSchedulerProperties
import xyz.candycrawler.draftsimparser.domain.set.model.ActiveSet
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension::class)
class ArticleParseRunnerTest {

    private class MutableTicker : Ticker {
        var nanos: Long = 0L
        override fun read(): Long = nanos
        fun advance(duration: Duration) { nanos += duration.toNanos() }
    }

    private val ticker = MutableTicker()
    private val fixedClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val schedulerProperties = ArticleParseSchedulerProperties(manualCooldown = Duration.ofHours(12))

    private val activeSetSource = mock<ActiveSetSource>()
    private val draftsimParseService = mock<DraftsimParseService>()
    private val parseAlertService = mock<ParseAlertService>()

    private val runner = ArticleParseRunner(
        activeSetSource = activeSetSource,
        draftsimParseService = draftsimParseService,
        parseAlertService = parseAlertService,
        schedulerProperties = schedulerProperties,
        clock = fixedClock,
        ticker = ticker,
    )

    @Test
    fun `triggerManual calls startScheduledParse for each active set`() {
        val setA = activeSet("BLB")
        val setB = activeSet("DSK")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(setA, setB))
        whenever(draftsimParseService.startScheduledParse(setA)).thenReturn(UUID.randomUUID())
        whenever(draftsimParseService.startScheduledParse(setB)).thenReturn(UUID.randomUUID())

        val result = runner.triggerManual()

        verify(draftsimParseService, times(1)).startScheduledParse(setA)
        verify(draftsimParseService, times(1)).startScheduledParse(setB)
        assert(result.tasks.containsKey("BLB"))
        assert(result.tasks.containsKey("DSK"))
    }

    @Test
    fun `triggerManual calls schedulerRunStarted with set codes`() {
        val setA = activeSet("BLB")
        val setB = activeSet("DSK")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(setA, setB))

        runner.triggerManual()

        verify(parseAlertService, times(1)).schedulerRunStarted(listOf("BLB", "DSK"))
    }

    @Test
    fun `tryRunScheduled is skipped within cooldown after triggerManual`() {
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(activeSet("BLB")))

        runner.triggerManual()
        clearInvocations(draftsimParseService)

        runner.tryRunScheduled()

        verify(draftsimParseService, never()).startScheduledParse(any())
        verify(parseAlertService, times(1)).schedulerSkippedDueToCooldown(any(), any())
    }

    @Test
    fun `tryRunScheduled runs after cooldown expires`() {
        val set = activeSet("BLB")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(set))

        runner.triggerManual()
        ticker.advance(schedulerProperties.manualCooldown.plusSeconds(1))

        runner.tryRunScheduled()

        verify(draftsimParseService, times(2)).startScheduledParse(set)
        verify(parseAlertService, never()).schedulerSkippedDueToCooldown(any(), any())
    }

    @Test
    fun `tryRunScheduled runs normally with no prior manual trigger`() {
        val set = activeSet("BLB")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(set))

        runner.tryRunScheduled()

        verify(draftsimParseService, times(1)).startScheduledParse(set)
        verify(parseAlertService, never()).schedulerSkippedDueToCooldown(any(), any())
    }

    @Test
    fun `exception in one set does not abort tryRunScheduled loop`() {
        val setA = activeSet("BLB")
        val setB = activeSet("DSK")
        whenever(activeSetSource.fetchActiveSets()).thenReturn(listOf(setA, setB))
        whenever(draftsimParseService.startScheduledParse(setA)).thenThrow(RuntimeException("network error"))

        runner.tryRunScheduled()

        verify(draftsimParseService, times(1)).startScheduledParse(setA)
        verify(draftsimParseService, times(1)).startScheduledParse(setB)
    }

    private fun activeSet(code: String, name: String = code) = ActiveSet(
        code = code,
        name = name,
        releasedAt = LocalDate.of(2024, 8, 2),
        setType = "expansion",
    )
}
