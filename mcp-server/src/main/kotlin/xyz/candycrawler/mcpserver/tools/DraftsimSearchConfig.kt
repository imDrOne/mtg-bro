package xyz.candycrawler.mcpserver.tools

internal const val DRAFTSIM_SIMILARITY_LOW = 0.50
internal const val DRAFTSIM_SIMILARITY_MEDIUM = 0.65
internal const val DRAFTSIM_SIMILARITY_HIGH = 0.80
internal const val DRAFTSIM_SIMILARITY_THRESHOLDS_ENV = "DRAFTSIM_SEMANTIC_SIMILARITY_THRESHOLDS"

internal val DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS = listOf(
    DRAFTSIM_SIMILARITY_HIGH,
    DRAFTSIM_SIMILARITY_MEDIUM,
    DRAFTSIM_SIMILARITY_LOW,
)

data class DraftsimSearchConfig(
    val semanticSimilarityThresholds: List<Double> = DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS,
)

internal fun draftsimSearchConfigFromEnv(getenv: (String) -> String? = System::getenv): DraftsimSearchConfig {
    val rawThresholds = getenv(DRAFTSIM_SIMILARITY_THRESHOLDS_ENV)
    val thresholds = parseDraftsimSimilarityThresholds(rawThresholds, DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS)
    if (!rawThresholds.isNullOrBlank() && !rawThresholds.hasValidSimilarityThreshold()) {
        System.err.println(
            "Warning: $DRAFTSIM_SIMILARITY_THRESHOLDS_ENV has no valid thresholds; " +
                "using defaults ${DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS.joinToString(",")}",
        )
    }
    return DraftsimSearchConfig(semanticSimilarityThresholds = thresholds)
}

internal fun parseDraftsimSimilarityThresholds(raw: String?, default: List<Double>): List<Double> {
    if (raw.isNullOrBlank()) return default
    val seen = mutableSetOf<Double>()
    val thresholds = raw.split(",")
        .mapNotNull { token ->
            val value = token.trim().toDoubleOrNull() ?: return@mapNotNull null
            if (value !in 0.0..1.0 || !seen.add(value)) return@mapNotNull null
            value
        }
    return thresholds.ifEmpty { default }
}

private fun String.hasValidSimilarityThreshold(): Boolean = split(",").any { token ->
    val value = token.trim().toDoubleOrNull() ?: return@any false
    value in 0.0..1.0
}
