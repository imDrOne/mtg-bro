package xyz.candycrawler.draftsimparser.domain.article.repository

import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.model.ArticlePage
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSearchFilter
import java.util.UUID

interface QueryArticleRepository {
    fun findById(id: Long): Article
    fun search(filter: ArticleSearchFilter, page: Int, pageSize: Int): ArticlePage
    fun findByTaskId(taskId: UUID): List<Article>
}
