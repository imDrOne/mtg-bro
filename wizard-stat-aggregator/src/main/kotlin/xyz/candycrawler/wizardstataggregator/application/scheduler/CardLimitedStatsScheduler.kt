package xyz.candycrawler.wizardstataggregator.application.scheduler

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsCollectionService
import xyz.candycrawler.wizardstataggregator.application.service.TrackedLimitedStatSetService

@Component
@ConditionalOnProperty(name = ["scheduler.card-limited-stats.enabled"], havingValue = "true")
class CardLimitedStatsScheduler(
    private val collectionService: CardLimitedStatsCollectionService,
    private val trackedSetService: TrackedLimitedStatSetService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scheduler.card-limited-stats.cron}")
    fun collect() {
        val activeSets = trackedSetService.findActive()
        if (activeSets.isEmpty()) {
            log.info("Scheduler {} triggered with no active tracked sets", javaClass.simpleName)
            return
        }

        log.info(
            "Scheduler {} triggered for {} active tracked sets: {}",
            javaClass.simpleName,
            activeSets.size,
            activeSets.joinToString { it.setCode },
        )

        runBlocking {
            activeSets.forEach { trackedSet ->
                runCatching {
                    collectionService.collectAll(trackedSet.setCode)
                }.onFailure { e ->
                    log.error(
                        "Failed scheduled card limited stats collection for set={}: {}",
                        trackedSet.setCode,
                        e.message,
                        e,
                    )
                }
            }
        }
    }
}
