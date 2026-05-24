package xyz.candycrawler.draftsimparser.application.scheduler

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.service.ArticleParseRunner

@Component
@ConditionalOnProperty(name = ["scheduler.article-parse.enabled"], havingValue = "true")
class ArticleParseScheduler(private val runner: ArticleParseRunner) {

    @Scheduled(cron = "\${scheduler.article-parse.cron}")
    fun run() = runner.tryRunScheduled()
}
