package xyz.candycrawler.draftsimparser.infrastructure.db.table

import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.Table

@OptIn(ExperimentalUuidApi::class)
object ParseTaskArticlesTable : Table("parse_task_articles") {
    val parseTaskId = reference("parse_task_id", ParseTasksTable.id)
    val articleId = reference("article_id", ArticlesTable)

    override val primaryKey = PrimaryKey(parseTaskId, articleId)
}
