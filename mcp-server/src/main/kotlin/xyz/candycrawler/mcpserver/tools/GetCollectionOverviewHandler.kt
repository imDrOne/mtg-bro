package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun getCollectionOverviewSchema() = ToolSchema(
    properties = buildJsonObject {},
    required = emptyList(),
)

suspend fun handleGetCollectionOverview(context: ToolContext, @Suppress("UNUSED_PARAMETER") request: CallToolRequest): CallToolResult {
    return try {
        val url = "${context.baseUrl}/api/v1/collection/overview"
        val response = context.httpClient.get(url).body<String>()

        val json = Json.parseToJsonElement(response).jsonObject

        val totalCards = json["totalCards"]?.jsonPrimitive?.int ?: 0

        val byColor = json["byColor"]?.jsonObject?.entries
            ?.joinToString(", ") { (color, count) -> "${color.ifEmpty { "Colorless" }}: ${count.jsonPrimitive.content}" }
            ?: ""

        val byType = json["byType"]?.jsonObject?.entries
            ?.joinToString(", ") { (type, count) -> "$type: ${count.jsonPrimitive.content}" }
            ?: ""

        val byRarity = json["byRarity"]?.jsonObject?.entries
            ?.joinToString(", ") { (rarity, count) -> "$rarity: ${count.jsonPrimitive.content}" }
            ?: ""

        val topTribes = json["topTribes"]?.jsonArray?.joinToString("\n") { tribeEl ->
            val tribe = tribeEl.jsonObject
            val name = tribe["name"]?.jsonPrimitive?.content ?: ""
            val count = tribe["count"]?.jsonPrimitive?.int ?: 0
            val colors = tribe["colors"]?.jsonPrimitive?.content?.ifEmpty { "Colorless" } ?: "Colorless"
            "  $name: $count cards ($colors)"
        } ?: ""

        val summary = buildString {
            appendLine("=== Collection Overview ===")
            appendLine("Total unique cards: $totalCards")
            appendLine()
            if (byColor.isNotEmpty()) appendLine("By color: $byColor")
            if (byType.isNotEmpty()) appendLine("By type: $byType")
            if (byRarity.isNotEmpty()) appendLine("By rarity: $byRarity")
            if (topTribes.isNotEmpty()) {
                appendLine()
                appendLine("Top tribes:")
                appendLine(topTribes)
            }
        }

        CallToolResult(content = listOf(TextContent(summary)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
