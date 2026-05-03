package xyz.candycrawler.mcpserver.tools

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun searchDraftsimArticlesSchema() = ToolSchema(
    properties = buildJsonObject {
        put("query", buildJsonObject {
            put("type", "string")
            put(
                "description",
                "Search query to find Draftsim articles. Examples: \"Merfolk\", \"BLB draft guide\", \"aggro strategy\"."
            )
        })
        put("page", buildJsonObject {
            put("type", "integer")
            put("description", "Page number (1-based, default 1)")
        })
        put("page_size", buildJsonObject {
            put("type", "integer")
            put("description", "Articles per page (default 10, max 20)")
        })
        put("types", buildJsonObject {
            put("type", "array")
            put("description", "Optional preview match insightType filter, for example [\"archetype\", \"mechanic\"].")
            put("items", buildJsonObject {
                put("type", "string")
            })
        })
        put("preview_limit", buildJsonObject {
            put("type", "integer")
            put("description", "Preview matches per semantic article result (default 3, max 5).")
        })
    },
    required = listOf("query"),
)

suspend fun handleSearchDraftsimArticles(
    context: ToolContext,
    request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
): CallToolResult {
    return try {
        val query = request.arguments?.get("query")?.jsonPrimitive?.content
            ?: return CallToolResult(
                content = listOf(TextContent("Error: query parameter is required")),
                isError = true
            )
        val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val pageSize = request.arguments?.get("page_size")?.jsonPrimitive?.content?.toIntOrNull() ?: 10
        val types = request.arguments?.get("types").toNormalizedTypeSet()
        val previewLimit =
            request.arguments?.get("preview_limit")?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(1, 5) ?: 3

        searchSemanticArticles(context, query, pageSize, types, previewLimit)?.let { return it }

        val url = "${context.draftsimParserBaseUrl}/api/v1/articles"
        val response = context.httpClient.get(url) {
            parameter("q", query)
            parameter("page", page)
            parameter("pageSize", pageSize.coerceIn(1, 20))
            parameter("favorite", true)
        }.body<String>()

        val summary = formatDraftsimFallbackSearchResponse(
            json = Json.parseToJsonElement(response).jsonObject,
            query = query,
            exhaustedSimilarityThresholds = context.draftsimSearchConfig.semanticSimilarityThresholds,
        )
            ?: return CallToolResult(content = listOf(TextContent("No articles found for query: $query")))
        CallToolResult(content = listOf(TextContent(summary)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}

private suspend fun searchSemanticArticles(
    context: ToolContext,
    query: String,
    pageSize: Int,
    types: Set<String>,
    previewLimit: Int,
): CallToolResult? =
    runCatching {
        val url = "${context.draftsimParserBaseUrl}/api/v1/articles/search/semantic"
        val topK = pageSize.coerceIn(1, 20)
        val summary = findDraftsimSemanticSummary(
            thresholds = context.draftsimSearchConfig.semanticSimilarityThresholds,
            search = { threshold ->
                val requestBody = buildJsonObject {
                    put("query", query)
                    put("topK", topK)
                    put("favorite", true)
                    put("similarityThreshold", threshold)
                }
                val response = context.httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }.body<String>()
                Json.parseToJsonElement(response).jsonObject
            },
            format = { json, threshold, attempt ->
                formatDraftsimSemanticSearchResponse(
                    json = json,
                    types = types,
                    previewLimit = previewLimit,
                    similarityThreshold = threshold,
                    semanticAttempts = attempt,
                )
            },
        ) ?: return null
        CallToolResult(
            content = listOf(TextContent(summary))
        )
    }.getOrNull()

internal suspend fun findDraftsimSemanticSummary(
    thresholds: List<Double>,
    search: suspend (Double) -> JsonObject,
    format: (JsonObject, Double, Int) -> String?,
): String? {
    thresholds.forEachIndexed { index, threshold ->
        val json = runCatching { search(threshold) }.getOrNull() ?: return@forEachIndexed
        val summary = format(json, threshold, index + 1)
        if (summary != null) return summary
    }
    return null
}
