package xyz.candycrawler.collectionmanager.infrastructure.db.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object DeckEntriesTable : LongIdTable("deck_entries") {
    val userId = long("user_id")
    val deckId = long("deck_id").references(DecksTable.id)
    val cardId = long("card_id").references(CardsTable.id)
    val quantity = integer("quantity")
    val isSideboard = bool("is_sideboard").default(false)

    init {
        uniqueIndex("uq_deck_entries_deck_card_sideboard", deckId, cardId, isSideboard)
    }
}
