package xyz.candycrawler.collectionmanager.infrastructure.db.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime

object CollectionEntriesTable : LongIdTable("collection_entries") {
    val userId = long("user_id")
    val cardId = long("card_id").references(CardsTable.id)
    val quantity = integer("quantity")
    val foil = bool("foil").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        uniqueIndex("uq_collection_entries_user_card_foil", userId, cardId, foil)
    }
}
