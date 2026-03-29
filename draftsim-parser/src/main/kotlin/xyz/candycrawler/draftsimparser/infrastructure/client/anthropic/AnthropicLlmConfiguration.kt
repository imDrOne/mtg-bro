package xyz.candycrawler.draftsimparser.infrastructure.client.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(prefix = "infrastructure.llm", name = ["client"], havingValue = "CLAUDE")
@Configuration
class AnthropicLlmConfiguration {

    @Bean
    fun anthropicClient(@Value("\${infrastructure.llm.anthropic.api-key}") apiKey: String): AnthropicClient =
        AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build()
}
