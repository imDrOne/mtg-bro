package xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSearchFilter
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSortField
import xyz.candycrawler.draftsimparser.domain.article.model.SortDirection
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ArticleRecord
import xyz.candycrawler.draftsimparser.infrastructure.db.table.ArticlesTable
import xyz.candycrawler.draftsimparser.infrastructure.db.table.ParseTaskArticlesTable
import xyz.candycrawler.draftsimparser.infrastructure.db.table.ParseTasksTable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Component
class ArticleSqlMapper {

    internal fun upsert(record: ArticleRecord): ArticleRecord = ArticlesTable.batchUpsert(
        listOf(record),
        keys = arrayOf(ArticlesTable.externalId),
        onUpdateExclude = listOf(ArticlesTable.keywords),
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
        this[ArticlesTable.keywords] = r.keywords
        // favorite, analyzedText, errorMsg, analyzStartedAt, analyzEndedAt
        // are NOT updated on conflict — preserved from existing row
    }.single().toRecord()

    internal fun selectById(id: Long): ArticleRecord? = ArticlesTable.selectAll()
        .where { ArticlesTable.id eq id }
        .map { it.toRecord() }
        .singleOrNull()

    internal fun search(filter: ArticleSearchFilter, limit: Int, offset: Long): List<ArticleRecord> {
        val q = ArticlesTable.selectAll()
        buildCondition(filter)?.let { q.where { it } }
        q.orderBy(buildSortExpression(filter.sortBy) to filter.sortDirection.toExposed())
        return q.limit(limit).offset(offset).map { it.toRecord() }
    }

    internal fun countSearch(filter: ArticleSearchFilter): Long {
        val q = ArticlesTable.selectAll()
        buildCondition(filter)?.let { q.where { it } }
        return q.count()
    }

    private fun buildCondition(filter: ArticleSearchFilter): Op<Boolean>? {
        var condition: Op<Boolean>? = null

        if (!filter.query.isNullOrBlank()) {
            val pattern = "%${filter.query.lowercase()}%"
            val textCondition = (ArticlesTable.title.lowerCase() like pattern) or
                (ArticlesTable.slug.lowerCase() like pattern)
            condition = textCondition
        }

        if (filter.favoriteOnly == true) {
            val favCondition = ArticlesTable.favorite eq true
            condition = condition?.and(favCondition) ?: favCondition
        }

        if (filter.setCode != null) {
            val setCondition = exists(
                (ParseTaskArticlesTable innerJoin ParseTasksTable)
                    .selectAll()
                    .where {
                        (ParseTaskArticlesTable.articleId eq ArticlesTable.id) and
                            (ParseTasksTable.keyword eq "set:${filter.setCode.lowercase()}")
                    }
            )
            condition = condition?.and(setCondition) ?: setCondition
        }

        if (filter.publishedFrom != null) {
            val fromCondition = ArticlesTable.publishedAt greaterEq filter.publishedFrom.atStartOfDay()
            condition = condition?.and(fromCondition) ?: fromCondition
        }

        if (filter.publishedTo != null) {
            val toCondition = ArticlesTable.publishedAt lessEq filter.publishedTo.plusDays(1).atStartOfDay()
            condition = condition?.and(toCondition) ?: toCondition
        }

        return condition
    }

    private fun buildSortExpression(sortBy: ArticleSortField): Expression<*> = when (sortBy) {
        ArticleSortField.PUBLISHED_AT -> ArticlesTable.publishedAt
        ArticleSortField.SET_CODE -> wrapAsExpression<String>(
            (ParseTasksTable innerJoin ParseTaskArticlesTable)
                .selectAll()
                .where {
                    (ParseTaskArticlesTable.articleId eq ArticlesTable.id) and
                        (ParseTasksTable.keyword like "set:%")
                }
                .orderBy(ParseTasksTable.keyword to SortOrder.ASC)
                .limit(1)
        )
    }

    private fun SortDirection.toExposed() = when (this) {
        SortDirection.ASC -> SortOrder.ASC
        SortDirection.DESC -> SortOrder.DESC
    }

    internal fun findByTaskId(taskId: UUID): List<ArticleRecord> = (ArticlesTable innerJoin ParseTaskArticlesTable)
        .selectAll()
        .where { ParseTaskArticlesTable.parseTaskId eq Uuid.parse(taskId.toString()) }
        .map { it.toRecord() }

    internal fun updateMutableFields(id: Long, record: ArticleRecord) {
        ArticlesTable.update({ ArticlesTable.id eq id }) {
            it[analyzedText] = record.analyzedText
            it[keywords] = record.keywords
            it[favorite] = record.favorite
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
        keywords = this[ArticlesTable.keywords],
        favorite = this[ArticlesTable.favorite],
        errorMsg = this[ArticlesTable.errorMsg],
        analyzStartedAt = this[ArticlesTable.analyzStartedAt],
        analyzEndedAt = this[ArticlesTable.analyzEndedAt],
        publishedAt = this[ArticlesTable.publishedAt],
        fetchedAt = this[ArticlesTable.fetchedAt],
    )
}
