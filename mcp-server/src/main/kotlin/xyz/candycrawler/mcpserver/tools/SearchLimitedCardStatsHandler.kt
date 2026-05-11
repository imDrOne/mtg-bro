package xyz.candycrawler.mcpserver.tools

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Locale

private const val PERCENT_MULTIPLIER = 100

fun searchLimitedCardStatsSchema() = ToolSchema(
    properties = buildJsonObject {
        put(
            "set_code",
            buildJsonObject {
                put("type", "string")
                put("description", "Set code, for example dmu, eoe, fin.")
            },
        )
        put(
            "match_type",
            buildJsonObject {
                put("type", "string")
                put("description", "17lands event type. Default: QuickDraft. Examples: QuickDraft, Sealed.")
            },
        )
        put(
            "names",
            buildJsonObject {
                put("type", "array")
                put("description", "Optional exact card names to fetch without loading the whole set.")
                put("items", buildJsonObject { put("type", "string") })
            },
        )
        put(
            "mtga_ids",
            buildJsonObject {
                put("type", "array")
                put("description", "Optional MTGA card IDs to fetch without loading the whole set.")
                put("items", buildJsonObject { put("type", "integer") })
            },
        )
        put(
            "tiers",
            buildJsonObject {
                put("type", "array")
                put("description", "Optional 17lands card grades: A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, F.")
                put("items", buildJsonObject { put("type", "string") })
            },
        )
        put(
            "min_win_rate",
            buildJsonObject {
                put("type", "number")
                put("description", "Minimum win rate as a decimal in [0, 1], for example 0.58.")
            },
        )
        put(
            "max_win_rate",
            buildJsonObject {
                put("type", "number")
                put("description", "Maximum win rate as a decimal in [0, 1], for example 0.62.")
            },
        )
        put(
            "sort",
            buildJsonObject {
                put("type", "string")
                put(
                    "description",
                    "Sort field: name, mtga_id, win_rate, game_count, drawn_improvement_win_rate. " +
                        "Default: win_rate.",
                )
            },
        )
        put(
            "sort_dir",
            buildJsonObject {
                put("type", "string")
                put("description", "Sort direction: asc or desc. Default: desc.")
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
                put("description", "Items per page (default 20, max 100).")
            },
        )
    },
    required = listOf("set_code"),
)

suspend fun handleSearchLimitedCardStats(
    context: ToolContext,
    request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest,
): CallToolResult = runCatching {
    val setCode = request.arguments?.get("set_code")?.jsonPrimitive?.contentOrNull
    if (setCode.isNullOrBlank()) {
        CallToolResult(content = listOf(TextContent("Error: set_code parameter is required")), isError = true)
    } else {
        val matchType = (request.arguments?.get("match_type")?.jsonPrimitive?.contentOrNull ?: "QuickDraft")
            .toLands17MatchType()
        val names = request.arguments?.get("names").stringList()
        val mtgaIds = request.arguments?.get("mtga_ids").intList()
        val tiers = request.arguments?.get("tiers").stringList()
        val minWinRate = request.arguments?.get("min_win_rate")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val maxWinRate = request.arguments?.get("max_win_rate")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val sort = request.arguments?.get("sort")?.jsonPrimitive?.contentOrNull ?: "win_rate"
        val sortDir = request.arguments?.get("sort_dir")?.jsonPrimitive?.contentOrNull ?: "desc"
        val page = request.arguments?.get("page")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        val pageSize = request.arguments?.get("page_size")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 20

        val httpResponse = context.httpClient.get("${context.wizardStatAggregatorBaseUrl}/api/v1/card-limited-stats") {
            parameter("set_code", setCode)
            parameter("match_type", matchType)
            names.forEach { parameter("names", it) }
            mtgaIds.forEach { parameter("mtga_ids", it) }
            tiers.forEach { parameter("tiers", it) }
            minWinRate?.let { parameter("min_win_rate", it) }
            maxWinRate?.let { parameter("max_win_rate", it) }
            parameter("sort", sort)
            parameter("sort_dir", sortDir)
            parameter("page", page.coerceAtLeast(1))
            parameter("page_size", pageSize.coerceIn(1, 100))
        }
        val response = httpResponse.readTextOrFail("wizard-stat-aggregator /api/v1/card-limited-stats")
        val json = Json.parseToJsonElement(response).jsonObject
        CallToolResult(content = listOf(TextContent(formatLimitedCardStatsResponse(json, setCode, matchType))))
    }
}.getOrElse { e ->
    CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
}

internal fun formatLimitedCardStatsResponse(json: JsonObject, setCode: String, matchType: String): String {
    val data = json["data"]?.jsonArray ?: JsonArray(emptyList())
    val totalStats = json["totalStats"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: data.size.toLong()
    val hasMore = json["hasMore"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
    val page = json["page"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
    val pageSize = json["pageSize"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: data.size

    if (data.isEmpty()) {
        return "No limited stats found for set=$setCode, match_type=$matchType."
    }

    val lines = data.mapIndexed { index, element ->
        val card = element.jsonObject
        val name = card["name"]?.jsonPrimitive?.contentOrNull ?: "?"
        val mtgaId = card["mtgaId"]?.jsonPrimitive?.contentOrNull ?: "?"
        val tier = card["tier"]?.jsonPrimitive?.contentOrNull ?: "-"
        val rarity = card["rarity"]?.jsonPrimitive?.contentOrNull ?: "?"
        val color = card["color"]?.jsonPrimitive?.contentOrNull ?: "?"
        val winRate = card["winRate"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val gameCount = card["gameCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val avgPick = card["avgPick"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val drawnWinRate = card["drawnWinRate"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val improvement = card["drawnImprovementWinRate"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

        buildString {
            append("${index + 1}. $name")
            append(" | mtga_id: $mtgaId")
            append(" | tier: $tier")
            append(" | $color $rarity")
            append(" | WR: ${winRate.formatPercent()}")
            if (gameCount != null) append(" | games: $gameCount")
            if (avgPick != null) append(" | ALSA: ${avgPick.formatDecimal()}")
            if (drawnWinRate != null) append(" | drawn WR: ${drawnWinRate.formatPercent()}")
            if (improvement != null) append(" | IWD: ${improvement.formatSignedPercent()}")
        }
    }

    return buildString {
        appendLine(
            "Limited stats for ${setCode.uppercase()} ($matchType): " +
                "returned=${data.size}, total=$totalStats, page=$page, page_size=$pageSize, has_more=$hasMore",
        )
        lines.forEach { appendLine(it) }
    }.trimEnd()
}

private fun JsonElement?.stringList(): List<String> = (this as? JsonArray).orEmpty()
    .mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }

private fun JsonElement?.intList(): List<Int> = (this as? JsonArray).orEmpty()
    .mapNotNull { it.jsonPrimitive.contentOrNull?.toIntOrNull() }
    .filter { it > 0 }
    .distinct()

private fun Double?.formatPercent(): String =
    this?.let { String.format(Locale.US, "%.1f%%", it * PERCENT_MULTIPLIER) } ?: "?"

private fun Double.formatSignedPercent(): String = String.format(Locale.US, "%+.1f%%", this * PERCENT_MULTIPLIER)

private fun Double.formatDecimal(): String = String.format(Locale.US, "%.2f", this)

private fun String.toLands17MatchType(): String = when (trim().lowercase().replace("_", "").replace("-", "")) {
    "quickdraft" -> "QuickDraft"
    "sealed" -> "Sealed"
    else -> trim()
}
