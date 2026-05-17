package xyz.candycrawler.draftsimparser.application.scheduler

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.port.ActiveSetSource
import xyz.candycrawler.draftsimparser.application.service.DraftsimParseService
import xyz.candycrawler.draftsimparser.application.service.ParseAlertService

@Component
@ConditionalOnProperty(name = ["scheduler.article-parse.enabled"], havingValue = "true")
class ArticleParseScheduler(
    private val activeSetSource: ActiveSetSource,
    private val draftsimParseService: DraftsimParseService,
    private val parseAlertService: ParseAlertService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scheduler.article-parse.cron}")
    fun run() {
        val sets = activeSetSource.fetchActiveSets()
        if (sets.isEmpty()) {
            log.info("Scheduler {} triggered with no active sets", javaClass.simpleName)
            return
        }

        log.info(
            "Scheduler {} triggered for {} active sets: {}",
            javaClass.simpleName,
            sets.size,
            sets.joinToString { it.code },
        )

        parseAlertService.schedulerRunStarted(sets.map { it.code })

        sets.forEach { set ->
            runCatching {
                draftsimParseService.startScheduledParse(set)
            }.onFailure { e ->
                log.error("Scheduled parse failed for set={}", set.code, e)
            }
        }
    }
}
