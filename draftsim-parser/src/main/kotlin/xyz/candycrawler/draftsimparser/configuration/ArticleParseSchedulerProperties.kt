package xyz.candycrawler.draftsimparser.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "scheduler.article-parse")
data class ArticleParseSchedulerProperties(val manualCooldown: Duration = DEFAULT_MANUAL_COOLDOWN) {
    companion object {
        val DEFAULT_MANUAL_COOLDOWN: Duration = Duration.ofHours(12)
    }
}
