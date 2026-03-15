package xyz.candycrawler.mcpserver.tools

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject

fun listScryfallFormatCodesSchema() = ToolSchema(properties = buildJsonObject { })

fun handleListScryfallFormatCodes(): CallToolResult {
    val text = """
        search_my_cards filters (use to narrow results, saves tokens):
        - colors: w,u,b,r,g — e.g. "wu" or "bg" for white-blue or black-green
        - color_identity: same format
        - type: creature, instant, sorcery, land, enchantment, artifact, planeswalker

        search_scryfall query syntax:
        - f:standard, f:commander, f:modern, f:pioneer, f:legacy, f:vintage
        - c:w, c:u, c:b, c:r, c:g — c:bg = black-green, c:wu = white-blue
        - id:wu = color identity white-blue
        - t:creature, t:instant, t:sorcery, t:land

        Examples:
        - search_my_cards with colors=bg, type=creature — black-green creatures in collection
        - search_scryfall "f:standard c:bg" — Standard legal black-green
    """.trimIndent()
    return CallToolResult(content = listOf(TextContent(text)))
}
