package xyz.candycrawler.wizardstataggregator.configuration.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class TelegramHttpClientConfig {

    @Bean
    fun telegramRestClient(@Value("\${infrastructure.http.client.telegram.base-url}") baseUrl: String): RestClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .build()
}
