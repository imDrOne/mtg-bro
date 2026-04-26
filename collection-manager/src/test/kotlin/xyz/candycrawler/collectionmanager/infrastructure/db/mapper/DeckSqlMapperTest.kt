package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.DeckSqlMapper
import xyz.candycrawler.collectionmanager.lib.AbstractIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Transactional
class DeckSqlMapperTest(
    @Autowired private val sqlMapper: DeckSqlMapper,
    @Autowired private val cardSqlMapper: CardSqlMapper,
) : AbstractIntegrationTest() {

    private val userId = 1L

    @Test
    fun `insert saves deck and returns record with generated id`() {
        val record = buildDeckRecord()
        val saved = sqlMapper.insert(record)

        assertNotNull(saved.id)
        assertEquals(userId, saved.userId)
        assertEquals("Test Deck", saved.name)
        assertEquals("STANDARD", saved.format)
        assertEquals(listOf("G"), saved.colorIdentity)
        assertNull(saved.comment)
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `insert saves comment when provided`() {
        val record = buildDeckRecord(comment = "Aggro strategy")
        val saved = sqlMapper.insert(record)

        assertEquals("Aggro strategy", saved.comment)
    }

    @Test
    fun `selectByIdAndUser returns null for unknown id`() {
        val result = sqlMapper.selectByIdAndUser(Long.MAX_VALUE, userId)
        assertNull(result)
    }

    @Test
    fun `selectByIdAndUser returns null for wrong userId`() {
        val saved = sqlMapper.insert(buildDeckRecord(name = "My Deck"))
        val result = sqlMapper.selectByIdAndUser(saved.id!!, 999L)
        assertNull(result)
    }

    @Test
    fun `selectByIdAndUser returns saved deck with correct fields`() {
        val saved = sqlMapper.insert(buildDeckRecord(name = "My Deck"))

        val found = sqlMapper.selectByIdAndUser(saved.id!!, userId)
        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertEquals(userId, found.userId)
        assertEquals("My Deck", found.name)
        assertEquals("STANDARD", found.format)
    }

    @Test
    fun `insertEntries saves mainboard and sideboard entries`() {
        val cardId = insertCard(setCode = "tst", collectorNumber = "1")
        val deckId = sqlMapper.insert(buildDeckRecord()).id!!

        sqlMapper.insertEntries(listOf(
            DeckEntryRecord(id = null, userId = userId, deckId = deckId, cardId = cardId, quantity = 4, isSideboard = false),
            DeckEntryRecord(id = null, userId = userId, deckId = deckId, cardId = cardId, quantity = 2, isSideboard = true),
        ))

        val entries = sqlMapper.selectEntriesByDeckId(deckId)
        assertEquals(2, entries.size)

        val mainboard = entries.first { !it.isSideboard }
        assertEquals(cardId, mainboard.cardId)
        assertEquals(4, mainboard.quantity)
        assertEquals(userId, mainboard.userId)

        val sideboard = entries.first { it.isSideboard }
        assertEquals(cardId, sideboard.cardId)
        assertEquals(2, sideboard.quantity)
    }

    @Test
    fun `selectEntriesByDeckId returns only entries for that deck`() {
        val cardId = insertCard(setCode = "tst", collectorNumber = "2")
        val deck1Id = sqlMapper.insert(buildDeckRecord()).id!!
        val deck2Id = sqlMapper.insert(buildDeckRecord()).id!!

        sqlMapper.insertEntries(listOf(
            DeckEntryRecord(id = null, userId = userId, deckId = deck1Id, cardId = cardId, quantity = 1, isSideboard = false),
            DeckEntryRecord(id = null, userId = userId, deckId = deck2Id, cardId = cardId, quantity = 2, isSideboard = false),
        ))

        val deck1Entries = sqlMapper.selectEntriesByDeckId(deck1Id)
        assertEquals(1, deck1Entries.size)
        assertEquals(1, deck1Entries.single().quantity)
    }

    @Test
    fun `selectAllByUser returns decks for that user ordered by createdAt descending`() {
        val deck1 = sqlMapper.insert(buildDeckRecord(name = "First"))
        Thread.sleep(5)
        val deck2 = sqlMapper.insert(buildDeckRecord(name = "Second"))

        val all = sqlMapper.selectAllByUser(userId)
        assertTrue(all.size >= 2)
        val ids = all.map { it.id }
        val idx1 = ids.indexOf(deck1.id)
        val idx2 = ids.indexOf(deck2.id)
        assertTrue(idx2 < idx1, "More recently created deck should appear first")
    }

    @Test
    fun `selectAllByUser does not return decks of other users`() {
        sqlMapper.insert(buildDeckRecord(name = "My Deck"))
        sqlMapper.insert(buildDeckRecord(name = "Other User Deck", userId = 999L))

        val all = sqlMapper.selectAllByUser(userId)
        assertTrue(all.all { it.userId == userId }, "Only decks for userId=$userId expected")
        assertTrue(all.none { it.name == "Other User Deck" })
    }

    private fun buildDeckRecord(name: String = "Test Deck", comment: String? = null, userId: Long = this.userId) = DeckRecord(
        id = null,
        userId = userId,
        name = name,
        format = "STANDARD",
        colorIdentity = listOf("G"),
        comment = comment,
        createdAt = null,
        updatedAt = null,
    )

    private fun insertCard(setCode: String, collectorNumber: String): Long =
        cardSqlMapper.upsertBatch(listOf(buildCardRecord(setCode, collectorNumber))).single().id!!

    private fun buildCardRecord(setCode: String, collectorNumber: String) = CardRecord(
        id = null,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Test Card",
        lang = "en",
        layout = "normal",
        manaCost = "{G}",
        cmc = 1.0,
        typeLine = "Creature — Elf",
        oracleText = null,
        colors = listOf("G"),
        colorIdentity = listOf("G"),
        keywords = emptyList(),
        power = "1",
        toughness = "1",
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
