package xyz.candycrawler.mcpserver.tools

import io.ktor.client.HttpClient
import xyz.candycrawler.mcpserver.auth.ToolAccessConfigData

data class ToolContext(
    val baseUrl: String,
    val draftsimParserBaseUrl: String,
    val httpClient: HttpClient,
    val toolAccessConfig: ToolAccessConfigData,
)
