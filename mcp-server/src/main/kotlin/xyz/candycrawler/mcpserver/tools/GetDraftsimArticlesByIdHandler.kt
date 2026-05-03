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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun getDraftsimArticlesByIdSchema() = ToolSchema(
    properties = buildJsonObject {
        put(
            "ids",
            buildJsonObject {
                put("type", "array")
                put("description", "List of article IDs to fetch analyzed content for.")
                put(
                    "items",
                    buildJsonObject {
                        put("type", "integer")
                    },
                )
            },
        )
        put(
            "types",
            buildJsonObject {
                put("type", "array")
                put(
                    "description",
                    "Optional insight.type filter, for example [\"archetype\", \"mechanic\", \"set_context\"].",
                )
                put(
                    "items",
                    buildJsonObject {
                        put("type", "string")
                    },
                )
            },
        )
        put(
            "page",
            buildJsonObject {
                put("type", "integer")
                put("description", "Insight page number per article (1-based, default 1).")
            },
        )
        put(
            "page_size",
            buildJsonObject {
                put("type", "integer")
                put("description", "Insights per page per article (default 50, max 100).")
            },
        )
    },
    required = listOf("ids"),
)

suspend fun handleGetDraftsimArticlesById(
    context: ToolContext,
    request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest,
): CallToolResult = runCatching {
    val idsArray = request.arguments?.get("ids")?.jsonArray

    if (idsArray == null) {
        CallToolResult(content = listOf(TextContent("Error: ids parameter is required")), isError = true)
    } else {
        val ids = idsArray.map { it.jsonPrimitive.content.toLong() }
        if (ids.isEmpty()) {
            CallToolResult(content = listOf(TextContent("Error: ids must not be empty")), isError = true)
        } else {
            val types = request.arguments?.get("types").toNormalizedTypeSet()
            val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize =
                request.arguments?.get("page_size")?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(1, 100) ?: 50

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
                CallToolResult(content = listOf(TextContent("No articles found for the given IDs")))
            } else {
                val output = formatDraftsimArticlesReport(articles, types, page, pageSize)
                CallToolResult(content = listOf(TextContent(output)))
            }
        }
    }
}.getOrElse { e ->
    CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
}
