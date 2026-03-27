package xyz.candycrawler.draftsimparser.domain.article.exception

class ArticleInvalidException(reason: String) : RuntimeException("Invalid article: $reason")
