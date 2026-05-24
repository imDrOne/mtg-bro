package xyz.candycrawler.draftsimparser.application.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.ActiveSetSource
import xyz.candycrawler.draftsimparser.configuration.ArticleParseSchedulerProperties
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class ArticleParseRunner(
    private val activeSetSource: ActiveSetSource,
    private val draftsimParseService: DraftsimParseService,
    private val parseAlertService: ParseAlertService,
    private val schedulerProperties: ArticleParseSchedulerProperties,
    private val clock: Clock = Clock.systemUTC(),
    ticker: Ticker = Ticker.systemTicker(),
) {
    private val manualCooldown: Duration get() = schedulerProperties.manualCooldown

    data class TriggerResult(val tasks: Map<String, UUID?>)

    private val log = LoggerFactory.getLogger(javaClass)

    private val manualLatch: Cache<String, Instant> = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(manualCooldown)
        .ticker(ticker)
        .build()

    fun triggerManual(): TriggerResult {
        manualLatch.put(LATCH_KEY, Instant.now(clock))
        return runFlow()
    }

    fun tryRunScheduled() {
        manualLatch.getIfPresent(LATCH_KEY)?.let { lastManual ->
            log.info(
                "Scheduled parse skipped: manual trigger {} ago (cooldown {})",
                Duration.between(lastManual, Instant.now(clock)),
                manualCooldown,
            )
            parseAlertService.schedulerSkippedDueToCooldown(lastManual, manualCooldown)
            return
        }
        runFlow()
    }

    private fun runFlow(): TriggerResult {
        val sets = activeSetSource.fetchActiveSets()
        if (sets.isEmpty()) {
            log.info("ArticleParseRunner triggered with no active sets")
            return TriggerResult(emptyMap())
        }
        log.info(
            "ArticleParseRunner triggered for {} active sets: {}",
            sets.size,
            sets.joinToString { it.code },
        )
        parseAlertService.schedulerRunStarted(sets.map { it.code })
        val tasks = sets.associate { set ->
            set.code to runCatching { draftsimParseService.startScheduledParse(set) }
                .onFailure { e -> log.error("Parse failed for set={}", set.code, e) }
                .getOrNull()
        }
        return TriggerResult(tasks)
    }

    companion object {
        private const val LATCH_KEY = "manual"
    }
}
