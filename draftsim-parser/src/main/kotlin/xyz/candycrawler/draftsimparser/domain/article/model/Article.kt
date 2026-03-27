package xyz.candycrawler.draftsimparser.domain.article.model

import xyz.candycrawler.draftsimparser.domain.article.exception.ArticleInvalidException
import java.time.LocalDateTime

data class Article(
    val id: Long?,
    val externalId: Long,
    val title: String,
    val slug: String,
    val url: String,
    val htmlContent: String?,
    val textContent: String?,
    val publishedAt: LocalDateTime?,
    val fetchedAt: LocalDateTime?,
) {
    init {
        fun invalid(reason: String): Nothing = throw ArticleInvalidException(reason)

        if (title.isBlank()) invalid("title must not be blank")
        if (url.isBlank()) invalid("url must not be blank")
        if (slug.isBlank()) invalid("slug must not be blank")
    }
}
