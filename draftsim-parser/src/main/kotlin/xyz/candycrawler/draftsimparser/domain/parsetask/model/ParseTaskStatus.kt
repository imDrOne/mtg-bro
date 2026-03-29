package xyz.candycrawler.draftsimparser.domain.parsetask.model

enum class ParseTaskStatus {
    PENDING,
    SEARCHING,
    FETCHING_ARTICLES,
    ANALYZING,
    COMPLETED,
    FAILED,
}
