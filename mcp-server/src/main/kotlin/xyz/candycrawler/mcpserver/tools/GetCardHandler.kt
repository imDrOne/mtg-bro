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

fun getCardSchema() = ToolSchema(
    properties = buildJsonObject {
        put("set_code", buildJsonObject {
            put("type", "string")
            put("description", "Set code (e.g. neo, dmu)")
        })
        put("collector_number", buildJsonObject {
            put("type", "string")
            put("description", "Collector number")
        })
    },
    required = listOf("set_code", "collector_number"),
)

suspend fun handleGetCard(context: ToolContext, request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest): CallToolResult {
    return try {
        val setCode = request.arguments?.get("set_code")?.jsonPrimitive?.content
            ?: return CallToolResult(content = listOf(TextContent("Error: set_code required")), isError = true)
        val collectorNumber = request.arguments?.get("collector_number")?.jsonPrimitive?.content
            ?: return CallToolResult(content = listOf(TextContent("Error: collector_number required")), isError = true)

        val response = context.httpClient.get("${context.baseUrl}/api/v1/cards/search") {
            parameter("set", setCode)
            parameter("collector_number", collectorNumber)
            parameter("page_size", 1)
        }.body<String>()

        val json = Json.parseToJsonElement(response).jsonObject
        val data = json["data"]?.jsonArray
        val card = data?.firstOrNull()?.jsonObject ?: return CallToolResult(
            content = listOf(TextContent("Card not found: $setCode #$collectorNumber")),
            isError = true
        )

        val name = card["name"]?.jsonPrimitive?.content ?: "?"
        val manaCost = card["manaCost"]?.jsonPrimitive?.content ?: ""
        val typeLine = card["typeLine"]?.jsonPrimitive?.content ?: ""
        val oracleText = card["oracleText"]?.jsonPrimitive?.content ?: ""
        val rarity = card["rarity"]?.jsonPrimitive?.content ?: ""
        val imageUris = card["imageUris"]?.jsonObject
        val imageUrl = imageUris?.get("normal")?.jsonPrimitive?.content
        val coll = card["collection"]?.jsonObject
        val collStr = if (coll != null) {
            val nf = coll["quantityNonFoil"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val f = coll["quantityFoil"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            " — You have: $nf non-foil, $f foil"
        } else " — Not in collection"

        val text = buildString {
            appendLine("$name $manaCost")
            appendLine("$typeLine ($rarity)$collStr")
            if (oracleText.isNotEmpty()) appendLine(oracleText)
            if (imageUrl != null) appendLine("Image: $imageUrl")
        }
        CallToolResult(content = listOf(TextContent(text)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
