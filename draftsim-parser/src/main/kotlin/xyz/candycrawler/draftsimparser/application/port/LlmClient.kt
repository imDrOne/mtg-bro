package xyz.candycrawler.draftsimparser.application.port

interface LlmClient {
    suspend fun complete(prompt: String): String?
}
