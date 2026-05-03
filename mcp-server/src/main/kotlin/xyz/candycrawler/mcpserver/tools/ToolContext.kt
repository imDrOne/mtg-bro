package xyz.candycrawler.mcpserver.tools

import io.ktor.client.HttpClient

data class ToolContext(
    val baseUrl: String,
    val draftsimParserBaseUrl: String,
    val wizardStatAggregatorBaseUrl: String,
    val httpClient: HttpClient,
    val draftsimSearchConfig: DraftsimSearchConfig = DraftsimSearchConfig(),
)
