package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun searchMyCardsSchema() = ToolSchema(
    properties = buildJsonObject {
        put("q", buildJsonObject {
            put("type", "string")
            put(
                "description", """Free-text search. Matches against card name, type line (e.g. "merfolk", "instant", "legendary"), and oracle text (e.g. "draw a card", "flying"). Case-insensitive substring match. Examples: "merfolk" → all cards with Merfolk in type line or text; "draw a card" → all cards mentioning draw in oracle text; "legendary creature" → all legendary creatures. Note: q also matches type_line; combine with type= for type-only filtering."""
            )
        })
        put("set", buildJsonObject {
            put("type", "string")
            put("description", """Set code, e.g. "neo", "dmu", "fdn".""")
        })
        put("colors", buildJsonObject {
            put("type", "string")
            put("description", """Filter by mana colors in card cost: w,u,b,r,g. Cards must contain ALL these colors. Example: "wu" → cards with white AND blue in their colors.""")
        })
        put("color_identity", buildJsonObject {
            put("type", "string")
            put("description", """Filter by color identity (includes all mana symbols on the card, not just cost). Example: "wu" → cards playable in a WU commander deck.""")
        })
        put("type", buildJsonObject {
            put("type", "string")
            put("description", """Filter by card type (case-insensitive substring match on type line). Example: "creature", "instant", "enchantment". Use this for type-only filtering; q= also matches type_line but additionally searches name and oracle text.""")
        })
        put("rarity", buildJsonObject {
            put("type", "string")
            put("description", "Exact match: common, uncommon, rare, mythic")
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

        if (response.isBlank()) {
            return CallToolResult(
                content = listOf(TextContent("Error: collection-manager returned empty response. Token may be missing user_id claim — re-authenticate and retry.")),
                isError = true,
            )
        }
        val json = Json.parseToJsonElement(response).jsonObject
        val data = json["data"]?.jsonArray ?: emptyList()
        val totalCards = json["totalCards"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val hasMore = json["hasMore"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

        val lines = data.mapIndexed { i, el ->
            val card = el.jsonObject
            val name = card["name"]?.jsonPrimitive?.content ?: "?"
            val manaCost = card["manaCost"]?.jsonPrimitive?.content ?: ""
            val typeLine = card["typeLine"]?.jsonPrimitive?.content ?: ""
            val oracleText = card["oracleText"]?.jsonPrimitive?.content ?: ""
            val power = card["power"]?.jsonPrimitive?.content
            val toughness = card["toughness"]?.jsonPrimitive?.content
            val setCode = card["setCode"]?.jsonPrimitive?.content ?: ""
            val collectorNumber = card["collectorNumber"]?.jsonPrimitive?.content ?: ""
            val rarityVal = card["rarity"]?.jsonPrimitive?.content ?: ""
            val coll = card["collection"] as? JsonObject
            val nonFoil = coll?.get("quantityNonFoil")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val foil = coll?.get("quantityFoil")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val copies = when {
                nonFoil > 0 && foil > 0 -> "$nonFoil non-foil, $foil foil"
                foil > 0 -> "$foil foil"
                nonFoil > 0 -> "$nonFoil copies"
                else -> "not in collection"
            }
            val imageUrl = (card["imageUris"] as? JsonObject)?.get("normal")?.jsonPrimitive?.content ?: ""
            val priceUsd = (card["prices"] as? JsonObject)?.get("usd")?.jsonPrimitive?.content ?: ""
            val ptStr = if (power != null && toughness != null) "$power/$toughness" else "-"
            buildString {
                append("${i + 1}. $name ($setCode #$collectorNumber) [$rarityVal]")
                if (manaCost.isNotEmpty()) append(" | Cost: $manaCost")
                appendLine()
                appendLine("   $typeLine | P/T: $ptStr")
                if (oracleText.isNotEmpty()) appendLine("   Oracle: $oracleText")
                append("   Collection: $copies")
                if (priceUsd.isNotEmpty()) append(" | Price: \$$priceUsd")
                if (imageUrl.isNotEmpty()) append(" | Image: $imageUrl")
            }
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
