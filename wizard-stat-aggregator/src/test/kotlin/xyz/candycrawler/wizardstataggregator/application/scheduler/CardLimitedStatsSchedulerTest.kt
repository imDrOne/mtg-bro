package xyz.candycrawler.wizardstataggregator.application.scheduler

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.BDDMockito.willThrow
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsCollectionService
import xyz.candycrawler.wizardstataggregator.application.service.TrackedLimitedStatSetService
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class CardLimitedStatsSchedulerTest {

    @Mock
    private lateinit var collectionService: CardLimitedStatsCollectionService

    @Mock
    private lateinit var trackedSetService: TrackedLimitedStatSetService

    @InjectMocks
    private lateinit var scheduler: CardLimitedStatsScheduler

    @Test
    fun `collect does nothing when no active tracked sets exist`() {
        whenever(trackedSetService.findActive()).thenReturn(emptyList())

        scheduler.collect()

        then(collectionService).shouldHaveNoInteractions()
    }

    @Test
    fun `collect runs collection for every active tracked set`() = runBlocking {
        whenever(trackedSetService.findActive()).thenReturn(
            listOf(
                TrackedLimitedStatSet("BLB", LocalDate.of(2026, 5, 1)),
                TrackedLimitedStatSet("DMU", LocalDate.of(2026, 5, 1)),
            )
        )

        scheduler.collect()

        then(collectionService).should().collectAll("BLB")
        then(collectionService).should().collectAll("DMU")
    }

    @Test
    fun `collect continues when one tracked set fails`() = runBlocking {
        whenever(trackedSetService.findActive()).thenReturn(
            listOf(
                TrackedLimitedStatSet("BLB", LocalDate.of(2026, 5, 1)),
                TrackedLimitedStatSet("DMU", LocalDate.of(2026, 5, 1)),
            )
        )
        willThrow(RuntimeException("boom")).given(collectionService).collectAll("BLB")

        scheduler.collect()

        then(collectionService).should().collectAll("DMU")
    }
}

