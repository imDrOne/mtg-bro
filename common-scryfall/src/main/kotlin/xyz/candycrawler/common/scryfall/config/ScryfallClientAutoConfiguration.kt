package xyz.candycrawler.common.scryfall.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import xyz.candycrawler.common.scryfall.client.ScryfallApiClient
import xyz.candycrawler.common.scryfall.config.interceptor.LoggingClientHttpRequestInterceptor
import xyz.candycrawler.common.scryfall.config.interceptor.RetryClientHttpRequestInterceptor

@AutoConfiguration
@EnableConfigurationProperties(ScryfallClientProperties::class)
class ScryfallClientAutoConfiguration(private val props: ScryfallClientProperties) {

    @Bean
    @ConditionalOnMissingBean
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
            .defaultHeader("User-Agent", "MtgBroApp/1.0")
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .configureMessageConverters { it.addCustomConverter(jsonConverter).build() }
            .requestInterceptors { interceptors ->
                interceptors.add(
                    RetryClientHttpRequestInterceptor(
                        maxAttempts = props.retry.maxAttempts,
                        initialDelayMs = props.retry.initialDelayMs,
                        multiplier = props.retry.multiplier,
                        maxDelayMs = props.retry.maxDelayMs,
                    ),
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
