package xyz.candycrawler.collectionmanager.configuration.client

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import xyz.candycrawler.collectionmanager.configuration.client.interceptor.LoggingClientHttpRequestInterceptor
import xyz.candycrawler.collectionmanager.configuration.client.interceptor.RetryClientHttpRequestInterceptor
import xyz.candycrawler.collectionmanager.configuration.client.property.ScryfallHttpClientProperties
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient

@Configuration
class ScryfallHttpClientConfig(
    private val props: ScryfallHttpClientProperties,
) {

    @Bean
    fun scryfallApiClient(): ScryfallApiClient {
        val jsonMapper = jsonMapper {
            addModule(kotlinModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        }

        val jsonConverter = JacksonJsonHttpMessageConverter(jsonMapper).apply {
            supportedMediaTypes = listOf(MediaType.APPLICATION_JSON)
        }

        val restClient = RestClient.builder()
            .baseUrl(props.baseUrl)
            .defaultHeader("User-Agent", "MtgBroCollectionManager/1.0")
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .configureMessageConverters { it.addCustomConverter(jsonConverter).build() }
            .requestInterceptors { interceptors ->
                interceptors.add(
                    RetryClientHttpRequestInterceptor(
                        maxAttempts = props.retry.maxAttempts,
                        initialDelayMs = props.retry.initialDelayMs,
                        multiplier = props.retry.multiplier,
                        maxDelayMs = props.retry.maxDelayMs,
                    )
                )
                interceptors.add(LoggingClientHttpRequestInterceptor())
            }
            .build()

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient<ScryfallApiClient>()
    }
}
