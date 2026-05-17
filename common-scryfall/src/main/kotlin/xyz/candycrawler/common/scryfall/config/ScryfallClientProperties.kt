package xyz.candycrawler.common.scryfall.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scryfall.client")
data class ScryfallClientProperties(
    val baseUrl: String = "https://api.scryfall.com",
    val retry: RetryProperties = RetryProperties(),
) {
    data class RetryProperties(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 100,
        val multiplier: Double = 2.0,
        val maxDelayMs: Long = 2000,
    )
}
