package xyz.candycrawler.mcpserver.tools

import io.ktor.client.HttpClient

data class ToolContext(
    val baseUrl: String,
    val httpClient: HttpClient,
)
