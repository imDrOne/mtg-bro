package xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
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
            keys = arrayOf(CollectionEntriesTable.userId, CollectionEntriesTable.cardId, CollectionEntriesTable.foil),
            onUpdateExclude = listOf(CollectionEntriesTable.createdAt),
            shouldReturnGeneratedValues = false,
        ) { record ->
            this[CollectionEntriesTable.userId] = record.userId
            this[CollectionEntriesTable.cardId] = record.cardId
            this[CollectionEntriesTable.quantity] = record.quantity
            this[CollectionEntriesTable.foil] = record.foil
            this[CollectionEntriesTable.createdAt] = record.createdAt ?: now
            this[CollectionEntriesTable.updatedAt] = now
        }
    }

    internal fun selectByUserAndCardId(userId: Long, cardId: Long): List<CollectionEntryRecord> =
        CollectionEntriesTable.selectAll()
            .where { (CollectionEntriesTable.userId eq userId) and (CollectionEntriesTable.cardId eq cardId) }
            .map { it.toRecord() }

    internal fun selectByUserAndCardIds(userId: Long, cardIds: List<Long>): List<CollectionEntryRecord> =
        if (cardIds.isEmpty()) emptyList()
        else CollectionEntriesTable.selectAll()
            .where { (CollectionEntriesTable.userId eq userId) and (CollectionEntriesTable.cardId inList cardIds) }
            .map { it.toRecord() }

    internal fun selectByUser(userId: Long): List<CollectionEntryRecord> =
        CollectionEntriesTable.selectAll()
            .where { CollectionEntriesTable.userId eq userId }
            .map { it.toRecord() }

    private fun ResultRow.toRecord(): CollectionEntryRecord = CollectionEntryRecord(
        id = this[CollectionEntriesTable.id].value,
        userId = this[CollectionEntriesTable.userId],
        cardId = this[CollectionEntriesTable.cardId],
        quantity = this[CollectionEntriesTable.quantity],
        foil = this[CollectionEntriesTable.foil],
        createdAt = this[CollectionEntriesTable.createdAt],
        updatedAt = this[CollectionEntriesTable.updatedAt],
    )
}
