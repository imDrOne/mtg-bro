package xyz.candycrawler.draftsimparser.infrastructure.client.anthropic

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.port.LlmClient

@ConditionalOnProperty(prefix = "infrastructure.llm", name = ["client"], havingValue = "MOCK")
@Component
class MockLlmClient : LlmClient {

    override suspend fun complete(prompt: String): String? = null
}
