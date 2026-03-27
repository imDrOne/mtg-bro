package xyz.candycrawler.draftsimparser.domain.article.repository

import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.util.UUID

interface ArticleRepository {
    fun save(article: Article): Article
    fun saveTaskArticleLink(taskId: UUID, articleId: Long)
}
