package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun listDraftsimArticlesSchema() = ToolSchema(
    properties = buildJsonObject {
        put(
            "q",
            buildJsonObject {
                put("type", "string")
                put("description", "Optional keyword query for Draftsim article title, slug, or text search.")
            },
        )
        put(
            "page",
            buildJsonObject {
                put("type", "integer")
                put("description", "Page number (1-based, default 1).")
            },
        )
        put(
            "page_size",
            buildJsonObject {
                put("type", "integer")
                put("description", "Articles per page (default 20, max 100).")
            },
        )
        put(
            "favorite",
            buildJsonObject {
                put("type", "boolean")
                put("description", "Whether to list only favorited articles (default true).")
            },
        )
    },
    required = emptyList(),
)

internal data class DraftsimArticleListOptions(
    val query: String?,
    val page: Int,
    val pageSize: Int,
    val favorite: Boolean,
)

suspend fun handleListDraftsimArticles(context: ToolContext, request: CallToolRequest): CallToolResult {
    return runCatching {
        val options = parseDraftsimArticleListOptions(request.arguments)

        val response = context.httpClient.get("${context.draftsimParserBaseUrl}/api/v1/articles") {
            options.query?.let { parameter("q", it) }
            parameter("page", options.page)
            parameter("pageSize", options.pageSize)
            parameter("favorite", options.favorite)
        }.body<String>()

        val summary = formatDraftsimArticleList(Json.parseToJsonElement(response).jsonObject)
            ?: return CallToolResult(content = listOf(TextContent("No Draftsim articles found")))
        CallToolResult(content = listOf(TextContent(summary)))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}

internal fun parseDraftsimArticleListOptions(arguments: JsonObject?): DraftsimArticleListOptions =
    DraftsimArticleListOptions(
        query = arguments?.get("q")?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotBlank),
        page = arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        pageSize = arguments?.get("page_size")?.jsonPrimitive?.content?.toIntOrNull()
            ?.coerceIn(1, MAX_PAGE_SIZE) ?: DEFAULT_PAGE_SIZE,
        favorite = arguments?.get("favorite")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true,
    )

private const val DEFAULT_PAGE_SIZE = 20
private const val MAX_PAGE_SIZE = 100
