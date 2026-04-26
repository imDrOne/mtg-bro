package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.deck.model.Deck
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckEntry
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckFormat
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckRecord

@Component
class DeckRecordToDeckMapper {

    fun apply(record: DeckRecord, entries: List<DeckEntryRecord>): Deck = Deck(
        id = record.id,
        userId = record.userId,
        name = record.name,
        format = DeckFormat.valueOf(record.format),
        colorIdentity = record.colorIdentity,
        comment = record.comment,
        entries = entries.map { e ->
            DeckEntry(
                id = e.id,
                deckId = e.deckId,
                cardId = e.cardId,
                quantity = e.quantity,
                isSideboard = e.isSideboard,
            )
        },
        createdAt = record.createdAt,
        updatedAt = record.updatedAt,
    )
}
