package xyz.candycrawler.draftsimparser.infrastructure.client.springai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.port.LlmClient

@ConditionalOnProperty(prefix = "infrastructure.llm", name = ["client"], havingValue = "SPRING_AI")
@Component
class SpringAiLlmClient(
    chatClientBuilder: ChatClient.Builder,
) : LlmClient {

    private val chatClient = chatClientBuilder.build()

    override suspend fun complete(prompt: String): String? = withContext(Dispatchers.IO) {
        chatClient.prompt(prompt).call().content()
    }
}
