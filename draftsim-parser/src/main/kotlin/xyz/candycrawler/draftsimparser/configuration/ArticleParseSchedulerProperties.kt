package xyz.candycrawler.draftsimparser.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "scheduler.article-parse")
data class ArticleParseSchedulerProperties(
    val manualCooldown: Duration = Duration.ofHours(12),
)
