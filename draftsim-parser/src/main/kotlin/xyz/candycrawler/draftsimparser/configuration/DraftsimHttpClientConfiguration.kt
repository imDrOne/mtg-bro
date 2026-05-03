package xyz.candycrawler.draftsimparser.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class DraftsimHttpClientConfiguration {

    @Bean
    fun draftsimRestClient(@Value("\${infrastructure.http.client.draftsim.base-url}") baseUrl: String): RestClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .build()
}
