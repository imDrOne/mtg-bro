package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CollectionEntrySqlMapper
import xyz.candycrawler.collectionmanager.lib.AbstractIntegrationTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class CardSqlMapperFindByTribeTest(
    @Autowired private val cardSqlMapper: CardSqlMapper,
    @Autowired private val collectionEntrySqlMapper: CollectionEntrySqlMapper,
) : AbstractIntegrationTest() {

    private val userId = 1L

    @Test
    fun `returns only cards that are in the collection`() {
        val inCollection = saveCard(typeLine = "Creature — Merfolk Wizard")
        saveCard(typeLine = "Creature — Merfolk Rogue") // not added to collection

        addToCollection(inCollection.id!!)

        val result = cardSqlMapper.findByTribe(userId, "Merfolk")

        assertEquals(1, result.size)
        assertEquals(inCollection.id, result.single().id)
    }

    @Test
    fun `matches tribe in type_line`() {
        val card = saveCard(typeLine = "Creature — Merfolk Wizard", oracleText = "Flying.")
        addToCollection(card.id!!)

        val result = cardSqlMapper.findByTribe(userId, "Merfolk")

        assertEquals(1, result.size)
        assertTrue(result.single().typeLine.contains("Merfolk"))
    }

    @Test
    fun `matches tribe in oracle_text when not in type_line`() {
        val card = saveCard(typeLine = "Instant", oracleText = "All Merfolk you control get +1/+1.")
        addToCollection(card.id!!)

        val result = cardSqlMapper.findByTribe(userId, "Merfolk")

        assertEquals(1, result.size)
        assertEquals("Instant", result.single().typeLine)
    }

    @Test
    fun `does not return cards where tribe appears neither in type_line nor oracle_text`() {
        val card = saveCard(typeLine = "Creature — Goblin", oracleText = "Haste.")
        addToCollection(card.id!!)

        val result = cardSqlMapper.findByTribe(userId, "Merfolk")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `match is case-insensitive`() {
        val card = saveCard(typeLine = "Creature — MERFOLK WIZARD")
        addToCollection(card.id!!)

        val result = cardSqlMapper.findByTribe(userId, "merfolk")

        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicates card with both foil and non-foil collection entries`() {
        val card = saveCard(typeLine = "Creature — Merfolk")
        addToCollection(cardId = card.id!!, foil = false)
        addToCollection(cardId = card.id!!, foil = true)

        val result = cardSqlMapper.findByTribe(userId, "Merfolk")

        assertEquals(1, result.size)
        assertEquals(card.id, result.single().id)
    }

    @Test
    fun `returns all matching cards in collection`() {
        val creature = saveCard(typeLine = "Creature — Merfolk Noble")
        val kindred = saveCard(typeLine = "Kindred Instant — Merfolk", oracleText = "Counter target spell.")
        val support = saveCard(typeLine = "Sorcery", oracleText = "Merfolk creatures gain flying.")
        val unrelated = saveCard(typeLine = "Creature — Goblin", oracleText = "Haste.")

        addToCollection(creature.id!!)
        addToCollection(kindred.id!!)
        addToCollection(support.id!!)
        addToCollection(unrelated.id!!)

        val result = cardSqlMapper.findByTribe(userId, "Merfolk")

        assertEquals(3, result.size)
        val ids = result.map { it.id }.toSet()
        assertTrue(creature.id in ids)
        assertTrue(kindred.id in ids)
        assertTrue(support.id in ids)
    }

    private fun saveCard(typeLine: String = "Creature — Merfolk", oracleText: String? = null): CardRecord {
        val record = buildRecord(typeLine = typeLine, oracleText = oracleText)
        return cardSqlMapper.upsertBatch(listOf(record)).single()
    }

    private fun addToCollection(cardId: Long, foil: Boolean = false) {
        val now = LocalDateTime.now()
        collectionEntrySqlMapper.upsertBatch(
            listOf(
                CollectionEntryRecord(
                    id = null,
                    userId = userId,
                    cardId = cardId,
                    quantity = 1,
                    foil = foil,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
    }

    private var collectorCounter = 0

    private fun buildRecord(typeLine: String = "Creature — Merfolk", oracleText: String? = null): CardRecord =
        CardRecord(
            id = null,
            scryfallId = UUID.randomUUID(),
            oracleId = UUID.randomUUID(),
            name = "Test Card",
            lang = "en",
            layout = "normal",
            manaCost = "{U}",
            cmc = 2.0,
            typeLine = typeLine,
            oracleText = oracleText,
            colors = listOf("U"),
            colorIdentity = listOf("U"),
            keywords = emptyList(),
            power = "1",
            toughness = "1",
            loyalty = null,
            setCode = "tst",
            setName = "Test Set",
            collectorNumber = (++collectorCounter).toString(),
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
