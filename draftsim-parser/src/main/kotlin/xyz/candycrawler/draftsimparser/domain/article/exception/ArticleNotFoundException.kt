package xyz.candycrawler.draftsimparser.domain.article.exception

class ArticleNotFoundException(id: Long) : RuntimeException("Article with id=$id not found")
