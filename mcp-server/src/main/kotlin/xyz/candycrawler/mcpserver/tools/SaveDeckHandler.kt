package xyz.candycrawler.mcpserver.tools

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import xyz.candycrawler.mcpserver.tools.schema.arrayProp
import xyz.candycrawler.mcpserver.tools.schema.integerProp
import xyz.candycrawler.mcpserver.tools.schema.objectProp
import xyz.candycrawler.mcpserver.tools.schema.stringProp
import xyz.candycrawler.mcpserver.tools.schema.toolSchema

private val deckEntryItemSchema = objectProp(
    properties = mapOf(
        "setCode" to stringProp("""Set code, e.g. "lea", "m21""""),
        "collectorNumber" to stringProp("""Collector number, e.g. "161", "42""""),
        "quantity" to integerProp("Number of copies (1-4)"),
    ),
    required = listOf("setCode", "collectorNumber", "quantity"),
)

fun saveDeckSchema(): ToolSchema = toolSchema(
    required = listOf("name", "format", "mainboard"),
    props = mapOf(
        "name" to stringProp("Deck name"),
        "format" to stringProp(
            "Deck format: STANDARD (mainboard >= 60 cards), SEALED or DRAFT (mainboard >= 40 cards)",
            enum = listOf("STANDARD", "SEALED", "DRAFT"),
        ),
        "comment" to stringProp("Optional comment about the deck (tactics, strategy, etc.)"),
        "mainboard" to arrayProp(
            "Mainboard cards. Use setCode and collectorNumber from search_my_cards results " +
                "(shown as \"(set #num)\"). Max 4 copies per card.",
            items = deckEntryItemSchema,
        ),
        "sideboard" to arrayProp(
            "Sideboard cards (optional). Same format as mainboard.",
            items = deckEntryItemSchema,
        ),
    ),
)

suspend fun handleSaveDeck(context: ToolContext, request: CallToolRequest): CallToolResult = runCatching {
    val args = request.arguments

    if (args == null) {
        CallToolResult(content = listOf(TextContent("Error: no arguments provided")), isError = true)
    } else {
        val name = args["name"]?.jsonPrimitive?.content
        val format = args["format"]?.jsonPrimitive?.content

        when {
            name == null -> CallToolResult(
                content = listOf(TextContent("Error: 'name' is required")),
                isError = true,
            )

            format == null -> CallToolResult(
                content = listOf(TextContent("Error: 'format' is required")),
                isError = true,
            )

            else -> saveDeck(context, args, name, format)
        }
    }
}.getOrElse { e ->
    CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
}

private suspend fun saveDeck(
    context: ToolContext,
    args: Map<String, JsonElement>,
    name: String,
    format: String,
): CallToolResult {
    val comment = args["comment"]?.jsonPrimitive?.content

    val body = buildJsonObject {
        put("name", name)
        put("format", format)
        if (comment != null) put("comment", comment)
        put("mainboard", parseEntries(args, "mainboard"))
        put("sideboard", parseEntries(args, "sideboard"))
    }

    val response = context.httpClient.post("${context.baseUrl}/api/v1/decks") {
        contentType(ContentType.Application.Json)
        setBody(body.toString())
    }

    val responseText = response.bodyAsText()

    return when {
        response.status.value == UNPROCESSABLE_ENTITY_STATUS -> {
            val errorMessage = runCatching {
                Json.parseToJsonElement(responseText).jsonObject["message"]?.jsonPrimitive?.content
            }.getOrNull() ?: responseText
            CallToolResult(
                content = listOf(TextContent("Validation failed: $errorMessage")),
                isError = true,
            )
        }

        !response.status.value.toString().startsWith("2") ->
            CallToolResult(
                content = listOf(TextContent("Error ${response.status.value}: $responseText")),
                isError = true,
            )

        else -> formatSavedDeck(responseText, name, format)
    }
}

private const val UNPROCESSABLE_ENTITY_STATUS = 422

private fun parseEntries(args: Map<String, JsonElement>, key: String) = buildJsonArray {
    args[key]?.jsonArray?.forEach { el ->
        val obj = el.jsonObject
        val setCode = obj["setCode"]?.jsonPrimitive?.content ?: ""
        val collectorNumber = obj["collectorNumber"]?.jsonPrimitive?.content ?: ""
        val quantity = obj["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        add(
            buildJsonObject {
                put("setCode", JsonPrimitive(setCode))
                put("collectorNumber", JsonPrimitive(collectorNumber))
                put("quantity", JsonPrimitive(quantity))
            },
        )
    }
}

private fun formatSavedDeck(responseText: String, name: String, format: String): CallToolResult {
    val deck = Json.parseToJsonElement(responseText).jsonObject
    val deckId = deck["id"]?.jsonPrimitive?.content ?: "?"
    val deckName = deck["name"]?.jsonPrimitive?.content ?: name
    val deckFormat = deck["format"]?.jsonPrimitive?.content ?: format
    val colors = deck["colorIdentity"]?.jsonArray
        ?.joinToString("") { it.jsonPrimitive.content }
        ?.ifEmpty { "colorless" }
        ?: "colorless"
    val mainboardCount = deck["mainboard"]?.jsonArray
        ?.sumOf { it.jsonObject["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0 }
        ?: 0
    val sideboardCount = deck["sideboard"]?.jsonArray
        ?.sumOf { it.jsonObject["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0 }
        ?: 0

    return CallToolResult(
        content = listOf(
            TextContent(
                "Deck saved successfully!\n" +
                    "ID: $deckId | Name: $deckName | Format: $deckFormat | Colors: $colors\n" +
                    "Mainboard: $mainboardCount cards | Sideboard: $sideboardCount cards",
            ),
        ),
    )
}
