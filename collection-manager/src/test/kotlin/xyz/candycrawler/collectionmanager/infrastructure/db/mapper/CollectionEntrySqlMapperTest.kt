package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CollectionEntrySqlMapper
import xyz.candycrawler.collectionmanager.lib.AbstractIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Transactional
class CollectionEntrySqlMapperTest(
    @Autowired private val sqlMapper: CollectionEntrySqlMapper,
    @Autowired private val cardSqlMapper: CardSqlMapper,
) : AbstractIntegrationTest() {

    @Test
    fun `upsertBatch persists all fields correctly`() {
        val cardId = insertCard(setCode = "dsk", collectorNumber = "10")

        val record = CollectionEntryRecord(id = null, cardId = cardId, quantity = 3, createdAt = null, updatedAt = null)
        sqlMapper.upsertBatch(listOf(record))

        val result = sqlMapper.selectByCardId(cardId)
        assertNotNull(result)
        assertNotNull(result.id)
        assertEquals(cardId, result.cardId)
        assertEquals(3, result.quantity)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `upsertBatch persists multiple entries`() {
        val cardId1 = insertCard(setCode = "dsk", collectorNumber = "20")
        val cardId2 = insertCard(setCode = "dsk", collectorNumber = "21")
        val cardId3 = insertCard(setCode = "dsk", collectorNumber = "22")

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, cardId = cardId1, quantity = 1, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, cardId = cardId2, quantity = 2, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, cardId = cardId3, quantity = 4, createdAt = null, updatedAt = null),
        ))

        val all = sqlMapper.selectAll()
        val cardIds = all.map { it.cardId }
        assert(cardIds.containsAll(listOf(cardId1, cardId2, cardId3)))
    }

    @Test
    fun `upsertBatch on same card id updates quantity`() {
        val cardId = insertCard(setCode = "dsk", collectorNumber = "30")

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, cardId = cardId, quantity = 2, createdAt = null, updatedAt = null)
        ))
        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, cardId = cardId, quantity = 5, createdAt = null, updatedAt = null)
        ))

        val result = sqlMapper.selectByCardId(cardId)
        assertNotNull(result)
        assertEquals(5, result.quantity)
    }

    @Test
    fun `upsertBatch preserves original createdAt on update`() {
        val cardId = insertCard(setCode = "dsk", collectorNumber = "40")

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, cardId = cardId, quantity = 1, createdAt = null, updatedAt = null)
        ))
        val firstCreatedAt = sqlMapper.selectByCardId(cardId)!!.createdAt

        Thread.sleep(10)

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, cardId = cardId, quantity = 2, createdAt = null, updatedAt = null)
        ))
        val secondCreatedAt = sqlMapper.selectByCardId(cardId)!!.createdAt

        assertEquals(firstCreatedAt, secondCreatedAt)
    }

    @Test
    fun `selectByCardId returns null when not found`() {
        val result = sqlMapper.selectByCardId(Long.MAX_VALUE)

        assertNull(result)
    }

    @Test
    fun `selectAll returns all persisted entries`() {
        val cardId1 = insertCard(setCode = "ecl", collectorNumber = "50")
        val cardId2 = insertCard(setCode = "ecl", collectorNumber = "51")

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, cardId = cardId1, quantity = 1, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, cardId = cardId2, quantity = 3, createdAt = null, updatedAt = null),
        ))

        val all = sqlMapper.selectAll()
        val cardIds = all.map { it.cardId }
        assert(cardIds.containsAll(listOf(cardId1, cardId2)))
    }

    private fun insertCard(setCode: String, collectorNumber: String): Long =
        cardSqlMapper.upsertBatch(listOf(buildCardRecord(setCode, collectorNumber))).single().id!!

    private fun buildCardRecord(setCode: String, collectorNumber: String) =
        xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord(
            id = null,
            scryfallId = UUID.randomUUID(),
            oracleId = UUID.randomUUID(),
            name = "Test Card",
            lang = "en",
            layout = "normal",
            manaCost = "{1}{G}",
            cmc = 2.0,
            typeLine = "Creature — Elf",
            oracleText = null,
            colors = listOf("G"),
            colorIdentity = listOf("G"),
            keywords = emptyList(),
            power = "2",
            toughness = "2",
            loyalty = null,
            setCode = setCode,
            setName = "Test Set",
            collectorNumber = collectorNumber,
            rarity = "common",
            releasedAt = LocalDate.of(2024, 1, 1),
            imageUriSmall = null,
            imageUriNormal = null,
            imageUriLarge = null,
            imageUriPng = null,
            imageUriArtCrop = null,
            imageUriBorderCrop = null,
            priceUsd = null,
            priceUsdFoil = null,
            priceEur = null,
            priceEurFoil = null,
            flavorText = null,
            artist = null,
        )
}
