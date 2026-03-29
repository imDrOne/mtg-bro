package xyz.candycrawler.draftsimparser.domain.article.repository

import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.model.ArticlePage
import java.util.UUID

interface QueryArticleRepository {
    fun findById(id: Long): Article
    fun search(query: String?, page: Int, pageSize: Int, favoriteOnly: Boolean? = null): ArticlePage
    fun findByTaskId(taskId: UUID): List<Article>
}
