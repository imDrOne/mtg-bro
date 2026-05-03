package xyz.candycrawler.draftsimparser.infrastructure.db.table

import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object ParseTaskArticlesTable : Table("parse_task_articles") {
    val parseTaskId = reference("parse_task_id", ParseTasksTable.id)
    val articleId = reference("article_id", ArticlesTable)

    override val primaryKey = PrimaryKey(parseTaskId, articleId)
}
