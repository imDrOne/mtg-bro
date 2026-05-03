package xyz.candycrawler.wizardstataggregator.configuration.client.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infrastructure.http.client.lands-17")
data class Lands17HttpClientProperties(val baseUrl: String, val retry: RetryProperties = RetryProperties()) {
    data class RetryProperties(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 500,
        val multiplier: Double = 2.0,
        val maxDelayMs: Long = 5000,
    )
}
