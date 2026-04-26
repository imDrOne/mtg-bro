package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryRecordToCollectionEntryMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryToCollectionEntryRecordMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CollectionEntrySqlMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ExposedCollectionEntryRepositoryTest {

    private val sqlMapper: CollectionEntrySqlMapper = mock()
    private val toRecord: CollectionEntryToCollectionEntryRecordMapper = CollectionEntryToCollectionEntryRecordMapper()
    private val toDomain: CollectionEntryRecordToCollectionEntryMapper = CollectionEntryRecordToCollectionEntryMapper()

    private val repository = ExposedCollectionEntryRepository(sqlMapper, toRecord, toDomain)

    private val userId = 1L

    @Test
    fun `saveAll converts entries to records and calls upsertBatch`() {
        val entries = listOf(
            buildEntry(cardId = 1L, quantity = 3),
            buildEntry(cardId = 2L, quantity = 5),
        )
        val expectedRecords = entries.map(toRecord::apply)

        repository.saveAll(entries)

        verify(sqlMapper).upsertBatch(expectedRecords)
    }

    @Test
    fun `saveAll with empty list calls upsertBatch with empty list`() {
        repository.saveAll(emptyList())

        verify(sqlMapper).upsertBatch(emptyList())
    }

    @Test
    fun `findByUserAndCardId returns domain entries when records found`() {
        val records = listOf(
            buildRecord(id = 10L, userId = userId, cardId = 42L, quantity = 4),
        )
        whenever(sqlMapper.selectByUserAndCardId(userId, 42L)).thenReturn(records)

        val result = repository.findByUserAndCardId(userId, 42L)

        assertEquals(1, result.size)
        assertEquals(10L, result.single().id)
        assertEquals(42L, result.single().cardId)
        assertEquals(4, result.single().quantity)
    }

    @Test
    fun `findByUserAndCardId returns empty when not found`() {
        whenever(sqlMapper.selectByUserAndCardId(userId, 99L)).thenReturn(emptyList())

        val result = repository.findByUserAndCardId(userId, 99L)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `findByUser returns all mapped domains`() {
        val records = listOf(
            buildRecord(id = 1L, userId = userId, cardId = 10L, quantity = 2),
            buildRecord(id = 2L, userId = userId, cardId = 20L, quantity = 3),
            buildRecord(id = 3L, userId = userId, cardId = 30L, quantity = 1),
        )
        whenever(sqlMapper.selectByUser(userId)).thenReturn(records)

        val result = repository.findByUser(userId)

        assertEquals(3, result.size)
        assertEquals(listOf(10L, 20L, 30L), result.map { it.cardId })
        assertEquals(listOf(2, 3, 1), result.map { it.quantity })
    }

    @Test
    fun `findByUser returns empty list when no entries`() {
        whenever(sqlMapper.selectByUser(userId)).thenReturn(emptyList())

        val result = repository.findByUser(userId)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `findByUserAndCardIds returns entries for given card ids`() {
        val records = listOf(
            buildRecord(id = 1L, userId = userId, cardId = 10L, quantity = 2, foil = false),
            buildRecord(id = 2L, userId = userId, cardId = 10L, quantity = 1, foil = true),
            buildRecord(id = 3L, userId = userId, cardId = 20L, quantity = 3, foil = false),
        )
        whenever(sqlMapper.selectByUserAndCardIds(userId, listOf(10L, 20L))).thenReturn(records)

        val result = repository.findByUserAndCardIds(userId, listOf(10L, 20L))

        assertEquals(3, result.size)
        assertEquals(listOf(10L, 10L, 20L), result.map { it.cardId })
    }

    @Test
    fun `findByUserAndCardIds returns empty list when cardIds empty`() {
        val result = repository.findByUserAndCardIds(userId, emptyList())

        assertEquals(emptyList(), result)
    }

    private fun buildEntry(cardId: Long, quantity: Int): CollectionEntry =
        CollectionEntry(userId = userId, cardId = cardId, quantity = quantity)

    private fun buildRecord(
        id: Long,
        userId: Long,
        cardId: Long,
        quantity: Int,
        foil: Boolean = false,
    ): CollectionEntryRecord {
        val now = LocalDateTime.now()
        return CollectionEntryRecord(
            id = id,
            userId = userId,
            cardId = cardId,
            quantity = quantity,
            foil = foil,
            createdAt = now,
            updatedAt = now,
        )
    }
}
