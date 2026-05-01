package xyz.candycrawler.draftsimparser.application.rest.dto.request

data class SemanticArticleSearchRequest(
    val query: String,
    val topK: Int? = null,
    val similarityThreshold: Double? = null,
    val favorite: Boolean? = true,
)
