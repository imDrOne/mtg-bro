package xyz.candycrawler.wizardstataggregator.configuration.client.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scheduler.card-limited-stats")
data class CardLimitedStatsSchedulerProperties(val enabled: Boolean = false, val cron: String = "@daily")
