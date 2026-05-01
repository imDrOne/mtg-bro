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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun searchDraftsimArticlesSchema() = ToolSchema(
    properties = buildJsonObject {
        put("query", buildJsonObject {
            put("type", "string")
            put("description", "Search query to find Draftsim articles. Examples: \"Merfolk\", \"BLB draft guide\", \"aggro strategy\".")
        })
        put("page", buildJsonObject {
            put("type", "integer")
            put("description", "Page number (1-based, default 1)")
        })
        put("page_size", buildJsonObject {
            put("type", "integer")
            put("description", "Articles per page (default 10, max 20)")
        })
    },
    required = listOf("query"),
)

suspend fun handleSearchDraftsimArticles(context: ToolContext, request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest): CallToolResult {
    return try {
        val query = request.arguments?.get("query")?.jsonPrimitive?.content
            ?: return CallToolResult(content = listOf(TextContent("Error: query parameter is required")), isError = true)
        val page = request.arguments?.get("page")?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val pageSize = request.arguments?.get("page_size")?.jsonPrimitive?.content?.toIntOrNull() ?: 10

        searchSemanticArticles(context, query, pageSize)?.let { return it }

        val url = "${context.draftsimParserBaseUrl}/api/v1/articles"
        val response = context.httpClient.get(url) {
            parameter("q", query)
            parameter("page", page)
            parameter("pageSize", pageSize.coerceIn(1, 20))
            parameter("favorite", true)
        }.body<String>()

        val json = Json.parseToJsonElement(response).jsonObject
        val articles = json["articles"]?.jsonArray ?: emptyList()
        val totalArticles = json["totalArticles"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val hasMore = json["hasMore"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

        if (articles.isEmpty()) {
            return CallToolResult(content = listOf(TextContent("No articles found for query: $query")))
        }

        val lines = articles.map { el ->
            val article = el.jsonObject
            val id = article["id"]?.jsonPrimitive?.content ?: "?"
            val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
            val slug = article["slug"]?.jsonPrimitive?.content ?: ""
            val publishedAt = article["publishedAt"]?.jsonPrimitive?.content?.take(10) ?: ""
            "[id=$id] $title — $slug ($publishedAt)"
        }

        val summary = buildString {
            appendLine("Found ${articles.size} articles (total: $totalArticles, hasMore: $hasMore)")
            appendLine()
            lines.forEach { appendLine(it) }
        }
        CallToolResult(content = listOf(TextContent(summary)))
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}

private suspend fun searchSemanticArticles(context: ToolContext, query: String, pageSize: Int): CallToolResult? =
    runCatching {
        val url = "${context.draftsimParserBaseUrl}/api/v1/articles/search/semantic"
        val requestBody = buildJsonObject {
            put("query", query)
            put("topK", pageSize.coerceIn(1, 20))
            put("favorite", true)
        }
        val response = context.httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }.body<String>()

        val results = Json.parseToJsonElement(response).jsonObject["results"]?.jsonArray ?: return null
        if (results.isEmpty()) return null

        val lines = results.map { el ->
            val result = el.jsonObject
            val article = result["article"]?.jsonObject ?: buildJsonObject { }
            val id = article["id"]?.jsonPrimitive?.content ?: "?"
            val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
            val slug = article["slug"]?.jsonPrimitive?.content ?: ""
            val publishedAt = article["publishedAt"]?.jsonPrimitive?.contentOrNull?.take(10) ?: ""
            val score = result["score"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val matches = result["matches"]?.jsonArray.orEmpty()
            val snippets = matches.take(2).mapNotNull { matchEl ->
                val match = matchEl.jsonObject
                val subject = match["subject"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                val insightType = match["insightType"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                val content = match["content"]?.jsonPrimitive?.contentOrNull
                    ?.lineSequence()
                    ?.firstOrNull { it.isNotBlank() }
                    ?.take(220)
                listOfNotNull(subject, insightType, content).takeIf { it.isNotEmpty() }?.joinToString(" — ")
            }

            buildString {
                append("[id=$id] $title — $slug ($publishedAt)")
                if (score != null) append(" score=${"%.3f".format(score)}")
                snippets.forEach { snippet ->
                    appendLine()
                    append("  match: $snippet")
                }
            }
        }

        CallToolResult(
            content = listOf(
                TextContent(
                    buildString {
                        appendLine("Found ${results.size} semantically relevant articles")
                        appendLine()
                        lines.forEach { appendLine(it) }
                    }
                )
            )
        )
    }.getOrNull()
