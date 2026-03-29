package xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ArticleRecord
import xyz.candycrawler.draftsimparser.infrastructure.db.table.ArticlesTable
import xyz.candycrawler.draftsimparser.infrastructure.db.table.ParseTaskArticlesTable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Component
class ArticleSqlMapper {

    internal fun upsert(record: ArticleRecord): ArticleRecord =
        ArticlesTable.batchUpsert(
            listOf(record),
            keys = arrayOf(ArticlesTable.externalId),
            shouldReturnGeneratedValues = true,
        ) { r ->
            this[ArticlesTable.externalId] = r.externalId
            this[ArticlesTable.title] = r.title
            this[ArticlesTable.slug] = r.slug
            this[ArticlesTable.url] = r.url
            this[ArticlesTable.htmlContent] = r.htmlContent
            this[ArticlesTable.textContent] = r.textContent
            this[ArticlesTable.publishedAt] = r.publishedAt
            this[ArticlesTable.fetchedAt] = r.fetchedAt
            // favorite, analyzedText, errorMsg, analyzStartedAt, analyzEndedAt
            // are NOT updated on conflict — preserved from existing row
        }.single().toRecord()

    internal fun selectById(id: Long): ArticleRecord? =
        ArticlesTable.selectAll()
            .where { ArticlesTable.id eq id }
            .map { it.toRecord() }
            .singleOrNull()

    internal fun search(query: String?, limit: Int, offset: Long, favoriteOnly: Boolean? = null): List<ArticleRecord> {
        val condition = buildSearchCondition(query, favoriteOnly)
        val q = ArticlesTable.selectAll()
        condition?.let { q.where { it } }
        return q.orderBy(ArticlesTable.publishedAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toRecord() }
    }

    internal fun countSearch(query: String?, favoriteOnly: Boolean? = null): Long {
        val condition = buildSearchCondition(query, favoriteOnly)
        val q = ArticlesTable.selectAll()
        condition?.let { q.where { it } }
        return q.count()
    }

    private fun buildSearchCondition(query: String?, favoriteOnly: Boolean?): Op<Boolean>? {
        var condition: Op<Boolean>? = null

        if (!query.isNullOrBlank()) {
            val pattern = "%${query.lowercase()}%"
            val textCondition = (ArticlesTable.title.lowerCase() like pattern) or
                (ArticlesTable.textContent.lowerCase() like pattern)
            condition = textCondition
        }

        if (favoriteOnly == true) {
            val favCondition = ArticlesTable.favorite eq true
            condition = condition?.let { it and favCondition } ?: favCondition
        }

        return condition
    }

    internal fun findByTaskId(taskId: UUID): List<ArticleRecord> =
        (ArticlesTable innerJoin ParseTaskArticlesTable)
            .selectAll()
            .where { ParseTaskArticlesTable.parseTaskId eq Uuid.parse(taskId.toString()) }
            .map { it.toRecord() }

    internal fun updateAnalysis(id: Long, record: ArticleRecord) {
        ArticlesTable.update({ ArticlesTable.id eq id }) {
            it[analyzedText] = record.analyzedText
            it[errorMsg] = record.errorMsg
            it[analyzStartedAt] = record.analyzStartedAt
            it[analyzEndedAt] = record.analyzEndedAt
        }
    }

    internal fun insertTaskArticleLink(taskId: UUID, articleId: Long) {
        ParseTaskArticlesTable.insertIgnore {
            it[parseTaskId] = Uuid.parse(taskId.toString())
            it[ParseTaskArticlesTable.articleId] = articleId
        }
    }

    private fun ResultRow.toRecord(): ArticleRecord = ArticleRecord(
        id = this[ArticlesTable.id].value,
        externalId = this[ArticlesTable.externalId],
        title = this[ArticlesTable.title],
        slug = this[ArticlesTable.slug],
        url = this[ArticlesTable.url],
        htmlContent = this[ArticlesTable.htmlContent],
        textContent = this[ArticlesTable.textContent],
        analyzedText = this[ArticlesTable.analyzedText],
        favorite = this[ArticlesTable.favorite],
        errorMsg = this[ArticlesTable.errorMsg],
        analyzStartedAt = this[ArticlesTable.analyzStartedAt],
        analyzEndedAt = this[ArticlesTable.analyzEndedAt],
        publishedAt = this[ArticlesTable.publishedAt],
        fetchedAt = this[ArticlesTable.fetchedAt],
    )
}
