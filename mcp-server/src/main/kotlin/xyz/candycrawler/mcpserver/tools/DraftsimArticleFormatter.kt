package xyz.candycrawler.mcpserver.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

internal fun formatDraftsimArticlesReport(
    articles: JsonArray,
    types: Set<String> = emptySet(),
    page: Int = 1,
    pageSize: Int = 50,
): String = articles.joinToString("\n\n") { el ->
    val article = el.jsonObject
    val id = article["id"]?.jsonPrimitive?.content ?: "?"
    val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
    val keywords = article["keywords"]?.jsonArray.orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
    val analyzedText = article["analyzedText"]?.jsonPrimitive?.contentOrNull
    val analysis = analyzedText?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
    val normalizedInsights = normalizeInsightObjects(analysis?.get("insights"))
        .filter { insight -> types.isEmpty() || insight["type"].normalizedType() in types }
    val safePage = page.coerceAtLeast(1)
    val safePageSize = pageSize.coerceIn(1, 100)
    val fromIndex = ((safePage - 1) * safePageSize).coerceAtMost(normalizedInsights.size)
    val pagedInsights = normalizedInsights.drop(fromIndex).take(safePageSize)
    val hasMore = fromIndex + pagedInsights.size < normalizedInsights.size

    buildString {
        appendLine("=== [$id] $title ===")
        appendLine("keywords: ${keywords.joinToString().ifBlank { "-" }}")
        appendLine("article_type: ${analysis?.get("article_type")?.jsonPrimitive?.contentOrNull ?: "-"}")
        appendLine("processing_profile: ${analysis?.get("processing_profile")?.jsonPrimitive?.contentOrNull ?: "-"}")
        appendLine(
            "pagination: total=${normalizedInsights.size}, page=$safePage, page_size=$safePageSize, has_more=$hasMore",
        )

        if (analysis == null) {
            appendLine("No analysis available")
            return@buildString
        }
        if (pagedInsights.isEmpty()) {
            appendLine("No insights on this page")
            return@buildString
        }

        pagedInsights.forEachIndexed { index, insight ->
            appendLine()
            appendLine("${fromIndex + index + 1}. ${formatInsightHeader(insight)}")
            INSIGHT_FIELD_ORDER.forEach { field ->
                insight[field].formatInsightValue()?.let { appendLine("   $field: $it") }
            }
            insight.keys
                .filterNot { it in INSIGHT_HEADER_FIELDS || it in INSIGHT_FIELD_ORDER }
                .sorted()
                .forEach { field ->
                    insight[field].formatInsightValue()?.let { appendLine("   $field: $it") }
                }
        }
    }.trimEnd()
}

internal fun formatDraftsimArticleList(json: JsonObject): String? {
    val articles = json["articles"]?.jsonArray ?: JsonArray(emptyList())
    if (articles.isEmpty()) return null
    val totalArticles = json["totalArticles"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
    val page = json["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
    val pageSize = json["pageSize"]?.jsonPrimitive?.content?.toIntOrNull() ?: articles.size
    val hasMore = json["hasMore"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

    return buildString {
        appendLine(
            "Found ${articles.size} Draftsim articles (total: $totalArticles, " +
                "page: $page, page_size: $pageSize, has_more: $hasMore)",
        )
        appendLine()
        articles.forEach { el ->
            val article = el.jsonObject
            val id = article["id"]?.jsonPrimitive?.content ?: "?"
            val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
            val slug = article["slug"]?.jsonPrimitive?.content ?: ""
            val url = article["url"]?.jsonPrimitive?.content ?: ""
            val publishedAt = article["publishedAt"]?.jsonPrimitive?.contentOrNull?.take(10) ?: ""
            val keywords = article["keywords"]?.jsonArray.orEmpty()
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
                .joinToString()
                .ifBlank { "-" }
            val favorite = article["favorite"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            val analyzed = !article["analyzedText"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()
            appendLine("[id=$id] $title - $slug ($publishedAt)")
            appendLine("  url: $url")
            appendLine("  keywords: $keywords")
            appendLine("  favorite: $favorite, analyzed: $analyzed")
        }
    }.trimEnd()
}

internal fun formatDraftsimSemanticSearchResponse(
    json: JsonObject,
    types: Set<String> = emptySet(),
    previewLimit: Int = 3,
    similarityThreshold: Double? = null,
    semanticAttempts: Int? = null,
): String? {
    val results = json["results"]?.jsonArray

    return if (results.isNullOrEmpty()) {
        null
    } else {
        val safePreviewLimit = previewLimit.coerceIn(1, 5)
        val lines = results.mapNotNull { el ->
            val result = el.jsonObject
            val article = result["article"]?.jsonObject ?: buildJsonObject { }
            val previews = result["matches"]?.jsonArray.orEmpty()
                .mapNotNull { it as? JsonObject }
                .filter { match -> types.isEmpty() || match["insightType"].normalizedType() in types }
                .take(safePreviewLimit)
                .mapIndexed { index, match -> formatPreviewMatch(index + 1, match) }

            if (types.isNotEmpty() && previews.isEmpty()) {
                null
            } else {
                formatSemanticArticleLine(result, article, previews)
            }
        }

        lines
            .takeUnless { it.isEmpty() }
            ?.let {
                buildSemanticSearchSummary(
                    lines = it,
                    safePreviewLimit = safePreviewLimit,
                    types = types,
                    similarityThreshold = similarityThreshold,
                    semanticAttempts = semanticAttempts,
                )
            }
    }
}

private fun formatSemanticArticleLine(result: JsonObject, article: JsonObject, previews: List<String>): String {
    val id = article["id"]?.jsonPrimitive?.content ?: "?"
    val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
    val slug = article["slug"]?.jsonPrimitive?.content ?: ""
    val publishedAt = article["publishedAt"]?.jsonPrimitive?.contentOrNull?.take(10) ?: ""
    val score = result["score"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

    return buildString {
        append("[id=$id] $title - $slug ($publishedAt)")
        if (score != null) append(" score=${String.format(Locale.US, "%.3f", score)}")
        previews.forEach { preview ->
            appendLine()
            append(preview)
        }
    }
}

private fun buildSemanticSearchSummary(
    lines: List<String>,
    safePreviewLimit: Int,
    types: Set<String>,
    similarityThreshold: Double?,
    semanticAttempts: Int?,
): String = buildString {
    appendLine("Found ${lines.size} semantically relevant articles")
    appendLine("preview_limit: $safePreviewLimit")
    if (similarityThreshold != null) {
        appendLine("similarity_threshold: ${formatSimilarityThreshold(similarityThreshold)}")
    }
    if (semanticAttempts != null) {
        appendLine("semantic_attempts: $semanticAttempts")
    }
    if (types.isNotEmpty()) appendLine("types: ${types.joinToString()}")
    appendLine()
    lines.forEach { appendLine(it) }
}.trimEnd()

internal fun formatDraftsimFallbackSearchResponse(
    json: JsonObject,
    query: String,
    exhaustedSimilarityThresholds: List<Double> = emptyList(),
): String? {
    val articles = json["articles"]?.jsonArray ?: JsonArray(emptyList())
    if (articles.isEmpty()) return null
    val totalArticles = json["totalArticles"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
    val hasMore = json["hasMore"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
    val lines = articles.map { el ->
        val article = el.jsonObject
        val id = article["id"]?.jsonPrimitive?.content ?: "?"
        val title = article["title"]?.jsonPrimitive?.content ?: "Untitled"
        val slug = article["slug"]?.jsonPrimitive?.content ?: ""
        val publishedAt = article["publishedAt"]?.jsonPrimitive?.content?.take(10) ?: ""
        "[id=$id] $title - $slug ($publishedAt)"
    }

    return buildString {
        appendLine("Found ${articles.size} articles (total: $totalArticles, hasMore: $hasMore)")
        appendLine("fallback: keyword article search for '$query'")
        if (exhaustedSimilarityThresholds.isNotEmpty()) {
            appendLine(
                "semantic_thresholds_exhausted: ${exhaustedSimilarityThresholds.joinToString {
                    formatSimilarityThreshold(it)
                }}",
            )
        }
        appendLine()
        lines.forEach { appendLine(it) }
    }.trimEnd()
}

internal fun JsonElement?.toNormalizedTypeSet(): Set<String> = (this as? JsonArray).orEmpty()
    .mapNotNull { it.normalizedType() }
    .toSet()

private fun normalizeInsightObjects(element: JsonElement?): List<JsonObject> = when (element) {
    is JsonObject -> listOf(element)
    is JsonArray -> element.flatMap { normalizeInsightObjects(it) }
    else -> emptyList()
}

private val INSIGHT_HEADER_FIELDS = setOf("type", "subject")
private val INSIGHT_FIELD_ORDER = listOf(
    "summary",
    "deckbuilding_implications",
    "related_cards",
    "mechanics",
    "archetypes",
    "formats",
    "tags",
    "confidence",
)

private fun formatInsightHeader(insight: JsonObject): String {
    val subject = insight["subject"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: "Untitled insight"
    val type = insight["type"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: "unknown"
    return "$subject (type=$type)"
}

private fun formatPreviewMatch(index: Int, match: JsonObject): String {
    val subject = match["subject"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: "-"
    val insightType = match["insightType"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: "-"
    val excerpt = match["content"]?.jsonPrimitive?.contentOrNull
        ?.collapseWhitespace()
        ?.take(220)
        ?.takeIf(String::isNotBlank)
        ?: "-"
    val tags = match["tags"]?.jsonArray.orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
        .joinToString()
        .ifBlank { "-" }
    return "  preview $index: subject=$subject; type=$insightType; excerpt=$excerpt; tags=$tags"
}

private fun JsonElement?.normalizedType(): String? = (this as? JsonPrimitive)?.contentOrNull
    ?.trim()
    ?.lowercase()
    ?.takeIf(String::isNotBlank)

private fun JsonElement?.formatInsightValue(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.collapseWhitespace()?.takeIf(String::isNotBlank)

    is JsonArray -> mapNotNull { it.formatInsightValue() }
        .takeIf { it.isNotEmpty() }
        ?.joinToString("; ")

    is JsonObject -> toString().collapseWhitespace().take(MAX_FORMATTED_JSON_VALUE_LENGTH).takeIf(String::isNotBlank)

    else -> null
}

private fun String.collapseWhitespace(): String = replace(Regex("\\s+"), " ").trim()

private const val MAX_FORMATTED_JSON_VALUE_LENGTH = 300

private fun formatSimilarityThreshold(value: Double): String = String.format(Locale.US, "%.2f", value)
