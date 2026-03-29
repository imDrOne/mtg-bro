package xyz.candycrawler.draftsimparser.infrastructure.client.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.port.LlmClient

@ConditionalOnProperty(prefix = "infrastructure.llm", name = ["client"], havingValue = "CLAUDE")
@Component
class AnthropicLlmClient(
    private val client: AnthropicClient,
) : LlmClient {

    override suspend fun complete(prompt: String): String? = withContext(Dispatchers.IO) {
        val response = client.messages().create(
            MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5_20251001)
                .maxTokens(1024)
                .messages(
                    listOf(
                        MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(prompt)
                            .build()
                    )
                )
                .build()
        )
        response.content().firstOrNull { it.isText() }?.asText()?.text()
    }
}
