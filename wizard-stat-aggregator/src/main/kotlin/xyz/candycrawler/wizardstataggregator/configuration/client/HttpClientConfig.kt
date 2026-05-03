package xyz.candycrawler.wizardstataggregator.configuration.client

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
import xyz.candycrawler.wizardstataggregator.configuration.client.interceptor.LoggingClientHttpRequestInterceptor
import xyz.candycrawler.wizardstataggregator.configuration.client.interceptor.RetryClientHttpRequestInterceptor
import xyz.candycrawler.wizardstataggregator.configuration.client.property.Lands17HttpClientProperties
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.Lands17ApiClient

@Configuration
class HttpClientConfig(private val props: Lands17HttpClientProperties) {

    @Bean
    fun lands17restApiClient(): Lands17ApiClient {
        val jsonMapper = jsonMapper {
            addModule(kotlinModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        }

        val jsonConverter = JacksonJsonHttpMessageConverter(jsonMapper).apply {
            supportedMediaTypes = listOf(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_HTML,
            )
        }

        val restClient = RestClient.builder()
            .baseUrl(props.baseUrl)
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
            .createClient<Lands17ApiClient>()
    }
}
