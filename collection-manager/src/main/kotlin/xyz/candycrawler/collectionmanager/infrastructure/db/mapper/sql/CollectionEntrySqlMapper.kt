package xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.table.CollectionEntriesTable
import java.time.LocalDateTime

@Component
class CollectionEntrySqlMapper {

    internal fun upsertBatch(records: List<CollectionEntryRecord>) {
        val now = LocalDateTime.now()
        CollectionEntriesTable.batchUpsert(
            records,
            keys = arrayOf(CollectionEntriesTable.cardId, CollectionEntriesTable.foil),
            onUpdateExclude = listOf(CollectionEntriesTable.createdAt),
            shouldReturnGeneratedValues = false,
        ) { record ->
            this[CollectionEntriesTable.cardId] = record.cardId
            this[CollectionEntriesTable.quantity] = record.quantity
            this[CollectionEntriesTable.foil] = record.foil
            this[CollectionEntriesTable.createdAt] = record.createdAt ?: now
            this[CollectionEntriesTable.updatedAt] = now
        }
    }

    internal fun selectByCardId(cardId: Long): CollectionEntryRecord? =
        CollectionEntriesTable.selectAll()
            .where { CollectionEntriesTable.cardId eq cardId }
            .map { it.toRecord() }
            .singleOrNull()

    internal fun selectByCardIds(cardIds: List<Long>): List<CollectionEntryRecord> =
        if (cardIds.isEmpty()) emptyList()
        else CollectionEntriesTable.selectAll()
            .where { CollectionEntriesTable.cardId inList cardIds }
            .map { it.toRecord() }

    internal fun selectAll(): List<CollectionEntryRecord> =
        CollectionEntriesTable.selectAll()
            .map { it.toRecord() }

    private fun ResultRow.toRecord(): CollectionEntryRecord = CollectionEntryRecord(
        id = this[CollectionEntriesTable.id].value,
        cardId = this[CollectionEntriesTable.cardId],
        quantity = this[CollectionEntriesTable.quantity],
        foil = this[CollectionEntriesTable.foil],
        createdAt = this[CollectionEntriesTable.createdAt],
        updatedAt = this[CollectionEntriesTable.updatedAt],
    )
}
