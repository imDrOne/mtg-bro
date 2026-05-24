package xyz.candycrawler.draftsimparser.domain.article.model

import java.time.LocalDate

data class ArticleSearchFilter(
    val query: String? = null,
    val favoriteOnly: Boolean? = null,
    val setCode: String? = null,
    val publishedFrom: LocalDate? = null,
    val publishedTo: LocalDate? = null,
    val sortBy: ArticleSortField = ArticleSortField.PUBLISHED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
)

enum class ArticleSortField { PUBLISHED_AT, SET_CODE }
enum class SortDirection { ASC, DESC }
