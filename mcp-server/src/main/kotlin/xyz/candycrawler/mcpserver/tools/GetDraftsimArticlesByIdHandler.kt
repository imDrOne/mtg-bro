package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun getDraftsimArticlesByIdSchema() = ToolSchema(
    properties = buildJsonObject {
        put("ids", buildJsonObject {
            put("type", "array")
            put("description", "List of article IDs to fetch analyzed content for.")
            put("items", buildJsonObject {
                put("type", "integer")
            })
        })
    },
    required = listOf("ids"),
)

suspend fun handleGetDraftsimArticlesById(context: ToolContext, request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest): CallToolResult {
    return try {
        val idsArray = request.arguments?.get("ids")?.jsonArray
            ?: return CallToolResult(content = listOf(TextContent("Error: ids parameter is required")), isError = true)

        val ids = idsArray.map { it.jsonPrimitive.content.toLong() }
        if (ids.isEmpty()) {
            return CallToolResult(content = listOf(TextContent("Error: ids must not be empty")), isError = true)
        }

        val url = "${context.draftsimParserBaseUrl}/api/v1/articles/by-ids"
        val requestBody = buildJsonObject {
            put("ids", JsonArray(ids.map { JsonPrimitive(it) }))
        }

        val response = context.httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }.body<String>()

        val articles = Json.parseToJsonElement(response).jsonArray

        if (articles.isEmpty()) {
            return CallToolResult(content = listOf(TextContent("No articles found for the given IDs")))
        }

        val output = articles.joinToString("\n\n") { el ->
            val article = el.jsonObject
            val id = article["id"]?.jsonPrimitive?.content ?: "?"
            val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
            val analyzedText = article["analyzedText"]?.jsonPrimitive?.content
            buildString {
                appendLine("=== [$id] $title ===")
                if (analyzedText != null) append(analyzedText) else append("No analysis available")
            }
        }

        CallToolResult(content = listOf(TextContent(output)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
