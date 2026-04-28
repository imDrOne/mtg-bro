package xyz.candycrawler.draftsimparser.infrastructure.db.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime

object ArticlesTable : LongIdTable("articles") {
    val externalId = long("external_id")
    val title = text("title")
    val slug = varchar("slug", 500)
    val url = text("url")
    val htmlContent = text("html_content").nullable()
    val textContent = text("text_content").nullable()
    val analyzedText = text("analyzed_text").nullable()
    val keywords = array<String>("keywords")
    val favorite = bool("favorite").default(false)
    val errorMsg = text("error_msg").nullable()
    val analyzStartedAt = datetime("analyz_started_at").nullable()
    val analyzEndedAt = datetime("analyz_end_at").nullable()
    val publishedAt = datetime("published_at").nullable()
    val fetchedAt = datetime("fetched_at").nullable()

    init {
        uniqueIndex("uq_articles_external_id", externalId)
    }
}
