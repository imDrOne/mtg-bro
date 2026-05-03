package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun searchScryfallSchema() = ToolSchema(
    properties = buildJsonObject {
        put(
            "query",
            buildJsonObject {
                put("type", "string")
                put("description", "Scryfall search query (required)")
            },
        )
        put(
            "unique",
            buildJsonObject {
                put("type", "string")
                put("description", "cards, art, or prints")
            },
        )
        put(
            "order",
            buildJsonObject {
                put("type", "string")
                put("description", "name, set, released, rarity, usd, cmc, etc.")
            },
        )
        put(
            "page",
            buildJsonObject {
                put("type", "integer")
                put("description", "Page number")
            },
        )
    },
    required = listOf("query"),
)

suspend fun handleSearchScryfall(
    context: ToolContext,
    request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest,
): CallToolResult {
    return runCatching {
        val query = request.arguments?.get("query")?.jsonPrimitive?.content
            ?: return CallToolResult(content = listOf(TextContent("Error: query is required")), isError = true)
        val unique = request.arguments?.get("unique")?.jsonPrimitive?.content
        val order = request.arguments?.get("order")?.jsonPrimitive?.content
        val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull()

        val url = "${context.baseUrl}/api/v1/scryfall/cards/search"
        val response = context.httpClient.get(url) {
            parameter("q", query)
            unique?.let { parameter("unique", it) }
            order?.let { parameter("order", it) }
            page?.let { parameter("page", it) }
        }.body<String>()

        val json = Json.parseToJsonElement(response).jsonObject
        val data = json["data"]?.jsonArray ?: emptyList()
        val totalCards = json["total_cards"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val hasMore = json["has_more"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

        val lines = data.take(50).mapIndexed { i, el ->
            val card = el.jsonObject
            val name = card["name"]?.jsonPrimitive?.content ?: "?"
            val manaCost = card["mana_cost"]?.jsonPrimitive?.content ?: ""
            val typeLine = card["type_line"]?.jsonPrimitive?.content ?: ""
            val rarity = card["rarity"]?.jsonPrimitive?.content ?: ""
            "${i + 1}. $name $manaCost — $typeLine ($rarity)"
        }

        val summary = buildString {
            appendLine("Found ${data.size} cards (total: $totalCards, hasMore: $hasMore)")
            lines.forEach { appendLine(it) }
            if (data.size > 50) appendLine("... (showing first 50)")
        }
        CallToolResult(content = listOf(TextContent(summary)))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
