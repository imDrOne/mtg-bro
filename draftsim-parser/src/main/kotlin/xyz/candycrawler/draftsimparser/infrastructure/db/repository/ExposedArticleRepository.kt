package xyz.candycrawler.draftsimparser.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.ArticleRecordToArticleMapper
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.ArticleToArticleRecordMapper
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql.ArticleSqlMapper
import java.util.UUID

@Repository
@Transactional
class ExposedArticleRepository(
    private val sqlMapper: ArticleSqlMapper,
    private val toDomain: ArticleRecordToArticleMapper,
    private val toRecord: ArticleToArticleRecordMapper,
) : ArticleRepository {

    override fun save(article: Article): Article =
        sqlMapper.upsert(toRecord.apply(article)).let(toDomain::apply)

    override fun saveTaskArticleLink(taskId: UUID, articleId: Long) =
        sqlMapper.insertTaskArticleLink(taskId, articleId)
}
