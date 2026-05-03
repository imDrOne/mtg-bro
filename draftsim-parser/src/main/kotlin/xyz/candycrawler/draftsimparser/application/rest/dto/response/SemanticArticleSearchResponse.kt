package xyz.candycrawler.draftsimparser.application.rest.dto.response

import xyz.candycrawler.draftsimparser.application.service.ArticleSemanticSearchMatch
import xyz.candycrawler.draftsimparser.application.service.ArticleSemanticSearchResult

data class SemanticArticleSearchResponse(val results: List<SemanticArticleSearchResultResponse>)

data class SemanticArticleSearchResultResponse(
    val article: ArticleSummaryResponse,
    val score: Double?,
    val matches: List<SemanticArticleSearchMatchResponse>,
)

data class SemanticArticleSearchMatchResponse(
    val score: Double?,
    val content: String,
    val insightType: String?,
    val subject: String?,
    val tags: List<String>,
)

fun List<ArticleSemanticSearchResult>.toSemanticSearchResponse() =
    SemanticArticleSearchResponse(results = map { it.toResponse() })

private fun ArticleSemanticSearchResult.toResponse() = SemanticArticleSearchResultResponse(
    article = article.toSummaryResponse(),
    score = score,
    matches = matches.map { it.toResponse() },
)

private fun ArticleSemanticSearchMatch.toResponse() = SemanticArticleSearchMatchResponse(
    score = score,
    content = content,
    insightType = insightType,
    subject = subject,
    tags = tags,
)
