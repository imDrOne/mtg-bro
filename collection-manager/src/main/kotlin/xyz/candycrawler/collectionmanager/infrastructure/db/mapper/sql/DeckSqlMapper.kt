package xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.table.DeckEntriesTable
import xyz.candycrawler.collectionmanager.infrastructure.db.table.DecksTable
import java.time.LocalDateTime

@Component
class DeckSqlMapper {

    internal fun insert(record: DeckRecord): DeckRecord {
        val now = LocalDateTime.now()
        val id = DecksTable.insertAndGetId {
            it[name] = record.name
            it[format] = record.format
            it[colorIdentity] = record.colorIdentity
            it[comment] = record.comment
            it[createdAt] = now
            it[updatedAt] = now
        }.value
        return record.copy(id = id, createdAt = now, updatedAt = now)
    }

    internal fun insertEntries(entries: List<DeckEntryRecord>) {
        if (entries.isEmpty()) return
        DeckEntriesTable.batchInsert(entries) { entry ->
            this[DeckEntriesTable.deckId] = entry.deckId
            this[DeckEntriesTable.cardId] = entry.cardId
            this[DeckEntriesTable.quantity] = entry.quantity
            this[DeckEntriesTable.isSideboard] = entry.isSideboard
        }
    }

    internal fun selectById(id: Long): DeckRecord? =
        DecksTable.selectAll()
            .where { DecksTable.id eq id }
            .map { it.toDeckRecord() }
            .singleOrNull()

    internal fun selectEntriesByDeckId(deckId: Long): List<DeckEntryRecord> =
        DeckEntriesTable.selectAll()
            .where { DeckEntriesTable.deckId eq deckId }
            .map { it.toEntryRecord() }

    internal fun selectAll(): List<DeckRecord> =
        DecksTable.selectAll()
            .orderBy(DecksTable.createdAt to SortOrder.DESC)
            .map { it.toDeckRecord() }

    private fun ResultRow.toDeckRecord() = DeckRecord(
        id = this[DecksTable.id].value,
        name = this[DecksTable.name],
        format = this[DecksTable.format],
        colorIdentity = this[DecksTable.colorIdentity],
        comment = this[DecksTable.comment],
        createdAt = this[DecksTable.createdAt],
        updatedAt = this[DecksTable.updatedAt],
    )

    private fun ResultRow.toEntryRecord() = DeckEntryRecord(
        id = this[DeckEntriesTable.id].value,
        deckId = this[DeckEntriesTable.deckId],
        cardId = this[DeckEntriesTable.cardId],
        quantity = this[DeckEntriesTable.quantity],
        isSideboard = this[DeckEntriesTable.isSideboard],
    )
}
