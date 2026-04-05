package xyz.candycrawler.collectionmanager.infrastructure.db.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime

object DecksTable : LongIdTable("decks") {
    val name = varchar("name", 255)
    val format = varchar("format", 20)
    val colorIdentity = array<String>("color_identity")
    val comment = text("comment").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
