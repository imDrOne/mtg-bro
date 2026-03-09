package xyz.candycrawler.collectionmanager.configuration.client.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infrastructure.http.client.scryfall")
data class ScryfallHttpClientProperties(
    val baseUrl: String,
    val retry: RetryProperties = RetryProperties(),
) {
    data class RetryProperties(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 100,
        val multiplier: Double = 2.0,
        val maxDelayMs: Long = 2000,
    )
}
