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
import kotlin.test.assertTrue

@Transactional
class CollectionEntrySqlMapperTest(
    @Autowired private val sqlMapper: CollectionEntrySqlMapper,
    @Autowired private val cardSqlMapper: CardSqlMapper,
) : AbstractIntegrationTest() {

    private val userId = 1L

    @Test
    fun `upsertBatch persists all fields correctly`() {
        val cardId = insertCard(setCode = "dsk", collectorNumber = "10")

        val record = CollectionEntryRecord(id = null, userId = userId, cardId = cardId, quantity = 3, foil = false, createdAt = null, updatedAt = null)
        sqlMapper.upsertBatch(listOf(record))

        val results = sqlMapper.selectByUserAndCardId(userId, cardId)
        assertEquals(1, results.size)
        val result = results.single()
        assertNotNull(result.id)
        assertEquals(userId, result.userId)
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
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId1, quantity = 1, foil = false, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId2, quantity = 2, foil = false, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId3, quantity = 4, foil = false, createdAt = null, updatedAt = null),
        ))

        val all = sqlMapper.selectByUser(userId)
        val cardIds = all.map { it.cardId }
        assert(cardIds.containsAll(listOf(cardId1, cardId2, cardId3)))
    }

    @Test
    fun `upsertBatch on same user+card+foil updates quantity`() {
        val cardId = insertCard(setCode = "dsk", collectorNumber = "30")

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId, quantity = 2, foil = false, createdAt = null, updatedAt = null)
        ))
        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId, quantity = 5, foil = false, createdAt = null, updatedAt = null)
        ))

        val results = sqlMapper.selectByUserAndCardId(userId, cardId)
        assertEquals(1, results.size)
        assertEquals(5, results.single().quantity)
    }

    @Test
    fun `upsertBatch preserves original createdAt on update`() {
        val cardId = insertCard(setCode = "dsk", collectorNumber = "40")

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId, quantity = 1, foil = false, createdAt = null, updatedAt = null)
        ))
        val firstCreatedAt = sqlMapper.selectByUserAndCardId(userId, cardId).single().createdAt

        Thread.sleep(10)

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId, quantity = 2, foil = false, createdAt = null, updatedAt = null)
        ))
        val secondCreatedAt = sqlMapper.selectByUserAndCardId(userId, cardId).single().createdAt

        assertEquals(firstCreatedAt, secondCreatedAt)
    }

    @Test
    fun `selectByUserAndCardId returns empty when not found`() {
        val result = sqlMapper.selectByUserAndCardId(userId, Long.MAX_VALUE)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `selectByUser returns all entries for that user only`() {
        val cardId1 = insertCard(setCode = "ecl", collectorNumber = "50")
        val cardId2 = insertCard(setCode = "ecl", collectorNumber = "51")
        val otherUserId = 999L

        sqlMapper.upsertBatch(listOf(
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId1, quantity = 1, foil = false, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, userId = userId, cardId = cardId2, quantity = 3, foil = false, createdAt = null, updatedAt = null),
            CollectionEntryRecord(id = null, userId = otherUserId, cardId = cardId1, quantity = 2, foil = false, createdAt = null, updatedAt = null),
        ))

        val all = sqlMapper.selectByUser(userId)
        val cardIds = all.map { it.cardId }
        assert(cardIds.containsAll(listOf(cardId1, cardId2)))
        assert(all.all { it.userId == userId }) { "Only userId=$userId entries expected" }
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
