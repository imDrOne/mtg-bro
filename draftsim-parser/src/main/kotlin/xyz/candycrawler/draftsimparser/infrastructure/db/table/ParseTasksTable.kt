package xyz.candycrawler.draftsimparser.infrastructure.db.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ParseTasksTable : Table("parse_tasks") {
    val id = uuid("id").clientDefault { Uuid.random() }
    val keyword = text("keyword")
    val status = varchar("status", 30)
    val totalArticles = integer("total_articles").nullable()
    val processedArticles = integer("processed_articles").default(0)
    val errorMessage = text("error_message").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
