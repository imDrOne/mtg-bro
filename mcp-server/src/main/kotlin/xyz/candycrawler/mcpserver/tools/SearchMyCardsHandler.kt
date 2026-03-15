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

fun searchMyCardsSchema() = ToolSchema(
    properties = buildJsonObject {
        put("q", buildJsonObject {
            put("type", "string")
            put("description", "Search query (name, type line, oracle text)")
        })
        put("set", buildJsonObject {
            put("type", "string")
            put("description", "Filter by set code (e.g. neo, dmu)")
        })
        put("colors", buildJsonObject {
            put("type", "string")
            put("description", "Filter by mana colors: w,u,b,r,g. E.g. 'wu' or 'w,u' for white-blue")
        })
        put("color_identity", buildJsonObject {
            put("type", "string")
            put("description", "Filter by color identity (same format as colors)")
        })
        put("type", buildJsonObject {
            put("type", "string")
            put("description", "Filter by card type: creature, instant, sorcery, land, enchantment, artifact, planeswalker")
        })
        put("rarity", buildJsonObject {
            put("type", "string")
            put("description", "Filter by rarity: common, uncommon, rare, mythic")
        })
        put("page", buildJsonObject {
            put("type", "integer")
            put("description", "Page number (1-based)")
        })
        put("page_size", buildJsonObject {
            put("type", "integer")
            put("description", "Items per page (max 175)")
        })
    },
)

suspend fun handleSearchMyCards(context: ToolContext, request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest): CallToolResult {
    return try {
        val q = request.arguments?.get("q")?.jsonPrimitive?.content
        val set = request.arguments?.get("set")?.jsonPrimitive?.content
        val colors = request.arguments?.get("colors")?.jsonPrimitive?.content
        val colorIdentity = request.arguments?.get("color_identity")?.jsonPrimitive?.content
        val type = request.arguments?.get("type")?.jsonPrimitive?.content
        val rarity = request.arguments?.get("rarity")?.jsonPrimitive?.content
        val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val pageSize = request.arguments?.get("page_size")?.jsonPrimitive?.content?.toIntOrNull() ?: 20

        val url = "${context.baseUrl}/api/v1/cards/search"
        val response = context.httpClient.get(url) {
            q?.let { parameter("q", it) }
            set?.let { parameter("set", it) }
            colors?.let { parameter("colors", it) }
            colorIdentity?.let { parameter("color_identity", it) }
            type?.let { parameter("type", it) }
            rarity?.let { parameter("rarity", it) }
            parameter("page", page)
            parameter("page_size", pageSize.coerceIn(1, 175))
        }.body<String>()

        val json = Json.parseToJsonElement(response).jsonObject
        val data = json["data"]?.jsonArray ?: emptyList()
        val totalCards = json["totalCards"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val hasMore = json["hasMore"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

        val lines = data.mapIndexed { i, el ->
            val card = el.jsonObject
            val name = card["name"]?.jsonPrimitive?.content ?: "?"
            val setCode = card["setCode"]?.jsonPrimitive?.content ?: ""
            val rarityVal = card["rarity"]?.jsonPrimitive?.content ?: ""
            val coll = card["collection"]?.jsonObject
            val nonFoil = coll?.get("quantityNonFoil")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val foil = coll?.get("quantityFoil")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val copies = when {
                nonFoil > 0 && foil > 0 -> "$nonFoil non-foil, $foil foil"
                foil > 0 -> "$foil foil"
                nonFoil > 0 -> "$nonFoil copies"
                else -> "not in collection"
            }
            "${i + 1}. $name ($setCode) $rarityVal — $copies"
        }

        val summary = buildString {
            appendLine("Found ${data.size} cards (total: $totalCards, hasMore: $hasMore)")
            lines.forEach { appendLine(it) }
        }
        CallToolResult(content = listOf(TextContent(summary)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
