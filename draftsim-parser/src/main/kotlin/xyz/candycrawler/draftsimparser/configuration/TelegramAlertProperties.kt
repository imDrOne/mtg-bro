package xyz.candycrawler.draftsimparser.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infrastructure.alerts.telegram")
data class TelegramAlertProperties(val perArticleSuccess: Boolean = false, val perArticleFailure: Boolean = true)
