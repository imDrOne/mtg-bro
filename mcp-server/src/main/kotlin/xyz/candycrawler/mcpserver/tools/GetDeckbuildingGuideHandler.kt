package xyz.candycrawler.mcpserver.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject

const val DECKBUILDING_GUIDE_RESOURCE_URI = "mtg-bro://guides/deckbuilding-skill.md"
private const val DECKBUILDING_GUIDE_RESOURCE_PATH = "/guides/deckbuilding/SKILL.md"
private const val DECKBUILDING_GUIDE_MIME_TYPE = "text/markdown"

fun getDeckbuildingGuideSchema() = ToolSchema(
    properties = buildJsonObject {},
    required = emptyList(),
)

fun handleGetDeckbuildingGuide(@Suppress("UNUSED_PARAMETER") request: CallToolRequest? = null): CallToolResult =
    CallToolResult(content = listOf(TextContent(loadDeckbuildingGuide())))

fun Server.addDeckbuildingGuideResource() {
    addResource(
        uri = DECKBUILDING_GUIDE_RESOURCE_URI,
        name = "mtg-bro-deckbuilding-skill",
        description = "Markdown guide for AI agents using mtg-bro MCP tools to build Magic: The Gathering decks.",
        mimeType = DECKBUILDING_GUIDE_MIME_TYPE,
    ) {
        ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = loadDeckbuildingGuide(),
                    uri = DECKBUILDING_GUIDE_RESOURCE_URI,
                    mimeType = DECKBUILDING_GUIDE_MIME_TYPE,
                )
            )
        )
    }
}

internal fun loadDeckbuildingGuide(): String =
    requireNotNull(object {}.javaClass.getResourceAsStream(DECKBUILDING_GUIDE_RESOURCE_PATH)) {
        "Deckbuilding guide resource not found: $DECKBUILDING_GUIDE_RESOURCE_PATH"
    }.bufferedReader().use { it.readText() }
