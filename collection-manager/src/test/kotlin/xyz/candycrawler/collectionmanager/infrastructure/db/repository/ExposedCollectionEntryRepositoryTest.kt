package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryRecordToCollectionEntryMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CollectionEntrySqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryToCollectionEntryRecordMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExposedCollectionEntryRepositoryTest {

    private val sqlMapper: CollectionEntrySqlMapper = mock()
    private val toRecord: CollectionEntryToCollectionEntryRecordMapper = CollectionEntryToCollectionEntryRecordMapper()
    private val toDomain: CollectionEntryRecordToCollectionEntryMapper = CollectionEntryRecordToCollectionEntryMapper()

    private val repository = ExposedCollectionEntryRepository(sqlMapper, toRecord, toDomain)

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
    fun `findByCardId returns domain when record found`() {
        val record = buildRecord(id = 10L, cardId = 42L, quantity = 4)
        whenever(sqlMapper.selectByCardId(42L)).thenReturn(record)

        val result = repository.findByCardId(42L)

        assertEquals(10L, result?.id)
        assertEquals(42L, result?.cardId)
        assertEquals(4, result?.quantity)
    }

    @Test
    fun `findByCardId returns null when record not found`() {
        whenever(sqlMapper.selectByCardId(99L)).thenReturn(null)

        val result = repository.findByCardId(99L)

        assertNull(result)
    }

    @Test
    fun `findAll returns all mapped domains`() {
        val records = listOf(
            buildRecord(id = 1L, cardId = 10L, quantity = 2),
            buildRecord(id = 2L, cardId = 20L, quantity = 3),
            buildRecord(id = 3L, cardId = 30L, quantity = 1),
        )
        whenever(sqlMapper.selectAll()).thenReturn(records)

        val result = repository.findAll()

        assertEquals(3, result.size)
        assertEquals(listOf(10L, 20L, 30L), result.map { it.cardId })
        assertEquals(listOf(2, 3, 1), result.map { it.quantity })
    }

    @Test
    fun `findAll returns empty list when no entries`() {
        whenever(sqlMapper.selectAll()).thenReturn(emptyList())

        val result = repository.findAll()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `findByCardIds returns entries for given card ids`() {
        val records = listOf(
            buildRecord(id = 1L, cardId = 10L, quantity = 2, foil = false),
            buildRecord(id = 2L, cardId = 10L, quantity = 1, foil = true),
            buildRecord(id = 3L, cardId = 20L, quantity = 3, foil = false),
        )
        whenever(sqlMapper.selectByCardIds(listOf(10L, 20L))).thenReturn(records)

        val result = repository.findByCardIds(listOf(10L, 20L))

        assertEquals(3, result.size)
        assertEquals(listOf(10L, 10L, 20L), result.map { it.cardId })
    }

    @Test
    fun `findByCardIds returns empty list when cardIds empty`() {
        val result = repository.findByCardIds(emptyList())

        assertEquals(emptyList(), result)
    }

    private fun buildEntry(cardId: Long, quantity: Int): CollectionEntry =
        CollectionEntry(id = null, cardId = cardId, quantity = quantity)

    private fun buildRecord(id: Long, cardId: Long, quantity: Int, foil: Boolean = false): CollectionEntryRecord {
        val now = LocalDateTime.now()
        return CollectionEntryRecord(
            id = id,
            cardId = cardId,
            quantity = quantity,
            foil = foil,
            createdAt = now,
            updatedAt = now,
        )
    }
}
