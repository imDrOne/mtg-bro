package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun analyzeTribalDepthSchema() = ToolSchema(
    properties = buildJsonObject {
        put("tribe", buildJsonObject {
            put("type", "string")
            put(
                "description",
                """Valid MTG creature subtype to analyze, e.g. "Merfolk", "Elf", "Goblin", "Zombie", "Dragon", "Human", "Kithkin".
                Must be an exact creature subtype as recognized by MTG rules.
                Returns: total owned cards, CMC distribution, role breakdown (creatures / kindred spells / tribal support),
                color spread, whether a lord/commander exists, and deck viability rating."""
            )
        })
    },
    required = listOf("tribe"),
)

suspend fun handleAnalyzeTribalDepth(context: ToolContext, request: CallToolRequest): CallToolResult {
    return try {
        val tribe = request.arguments?.get("tribe")?.jsonPrimitive?.content
            ?: return CallToolResult(content = listOf(TextContent("Error: 'tribe' parameter is required")), isError = true)

        val url = "${context.baseUrl}/api/v1/cards/tribal/$tribe"
        val response = context.httpClient.get(url).body<String>()

        val json = Json.parseToJsonElement(response).jsonObject

        val tribeVal = json["tribe"]?.jsonPrimitive?.content ?: tribe
        val totalCards = json["totalCards"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val deckViability = json["deckViability"]?.jsonPrimitive?.content ?: "unknown"
        val hasLord = json["hasLord"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val hasCommander = json["hasCommander"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

        val byRole = json["byRole"]?.jsonObject
        val creatures = byRole?.get("creatures")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val tribalSpells = byRole?.get("tribalSpells")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val tribalSupport = byRole?.get("tribalSupport")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        val byCmc = json["byCmc"]?.jsonObject?.entries
            ?.sortedWith(compareBy { if (it.key == "5+") 6 else it.key.toIntOrNull() ?: 0 })
            ?.joinToString(", ") { (cmc, count) -> "$cmc: ${count.jsonPrimitive.content}" }
            ?: ""

        val colorSpread = json["colorSpread"]?.jsonObject?.entries
            ?.sortedBy { it.key }
            ?.joinToString(", ") { (color, count) ->
                "${color.ifEmpty { "Colorless" }}: ${count.jsonPrimitive.content}"
            }
            ?: ""

        val summary = buildString {
            appendLine("=== Tribal Analysis: $tribeVal ===")
            appendLine("Total cards in collection: $totalCards")
            appendLine("Deck viability: $deckViability (strong ≥ 20 creatures, moderate ≥ 10, weak < 10)")
            appendLine()
            appendLine("By role:")
            appendLine("  Creatures: $creatures")
            appendLine("  Kindred spells: $tribalSpells")
            appendLine("  Tribal support (oracle text mentions): $tribalSupport")
            if (byCmc.isNotEmpty()) appendLine()
            if (byCmc.isNotEmpty()) appendLine("CMC distribution: $byCmc")
            if (colorSpread.isNotEmpty()) appendLine("Color spread: $colorSpread")
            appendLine()
            appendLine("Has lord (legendary tribal card): $hasLord")
            appendLine("Has commander (legendary creature of tribe): $hasCommander")
        }

        CallToolResult(content = listOf(TextContent(summary)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
