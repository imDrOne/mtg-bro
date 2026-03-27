package xyz.candycrawler.draftsimparser.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.draftsimparser.domain.article.exception.ArticleNotFoundException
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.model.ArticlePage
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.ArticleRecordToArticleMapper
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql.ArticleSqlMapper
import java.util.UUID

@Repository
@Transactional(readOnly = true)
class ExposedQueryArticleRepository(
    private val sqlMapper: ArticleSqlMapper,
    private val toDomain: ArticleRecordToArticleMapper,
) : QueryArticleRepository {

    override fun findById(id: Long): Article =
        sqlMapper.selectById(id)?.let(toDomain::apply)
            ?: throw ArticleNotFoundException(id)

    override fun search(query: String?, page: Int, pageSize: Int): ArticlePage {
        val offset = ((page - 1) * pageSize).toLong()
        val records = sqlMapper.search(query, pageSize, offset)
        val total = sqlMapper.countSearch(query)
        return ArticlePage(
            articles = records.map(toDomain::apply),
            totalArticles = total,
            page = page,
            pageSize = pageSize,
            hasMore = offset + pageSize < total,
        )
    }

    override fun findByTaskId(taskId: UUID): List<Article> =
        sqlMapper.findByTaskId(taskId).map(toDomain::apply)
}
