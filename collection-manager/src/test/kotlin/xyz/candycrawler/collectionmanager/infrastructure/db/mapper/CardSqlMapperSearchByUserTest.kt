package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.model.CardSortOrder
import xyz.candycrawler.collectionmanager.domain.card.model.SortDirection
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
class CardSqlMapperSearchByUserTest(
    @Autowired private val cardSqlMapper: CardSqlMapper,
    @Autowired private val collectionEntrySqlMapper: CollectionEntrySqlMapper,
) : AbstractIntegrationTest() {

    private val userId = 1L
    private val otherUserId = 2L

    @Test
    fun `returns only cards in user collection`() {
        val owned = saveCard(name = "Lightning Bolt")
        saveCard(name = "Island")
        addToCollection(userId, owned.id!!)

        val result = searchByUser(userId)

        assertEquals(1, result.size)
        assertEquals("Lightning Bolt", result.single().name)
    }

    @Test
    fun `does not return cards of other user`() {
        val card = saveCard()
        addToCollection(otherUserId, card.id!!)

        val result = searchByUser(userId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deduplicates foil and non-foil entries for same card`() {
        val card = saveCard()
        addToCollection(userId, card.id!!, foil = false)
        addToCollection(userId, card.id!!, foil = true)

        val result = searchByUser(userId)

        assertEquals(1, result.size)
    }

    @Test
    fun `countSearchByUser deduplicates foil and non-foil`() {
        val card = saveCard()
        addToCollection(userId, card.id!!, foil = false)
        addToCollection(userId, card.id!!, foil = true)

        val count = countByUser(userId)

        assertEquals(1L, count)
    }

    @Test
    fun `filters by query text in name`() {
        val bolt = saveCard(name = "Lightning Bolt")
        val island = saveCard(name = "Island")
        addToCollection(userId, bolt.id!!)
        addToCollection(userId, island.id!!)

        val result = searchByUser(userId, queryText = "lightning")

        assertEquals(1, result.size)
        assertEquals("Lightning Bolt", result.single().name)
    }

    @Test
    fun `filters by rarity`() {
        val rare = saveCard(name = "Rare Card", rarity = "rare")
        val common = saveCard(name = "Common Card", rarity = "common")
        addToCollection(userId, rare.id!!)
        addToCollection(userId, common.id!!)

        val result = searchByUser(userId, rarity = "rare")

        assertEquals(1, result.size)
        assertEquals("Rare Card", result.single().name)
    }

    @Test
    fun `filters by set code`() {
        val fdn = saveCard(name = "FDN Card", setCode = "fdn")
        val neo = saveCard(name = "NEO Card", setCode = "neo")
        addToCollection(userId, fdn.id!!)
        addToCollection(userId, neo.id!!)

        val result = searchByUser(userId, setCode = "fdn")

        assertEquals(1, result.size)
        assertEquals("FDN Card", result.single().name)
    }

    @Test
    fun `paginates correctly`() {
        repeat(5) { i ->
            val card = saveCard(name = "Card $i")
            addToCollection(userId, card.id!!)
        }

        val page1 = searchByUser(userId, limit = 2, offset = 0)
        val page2 = searchByUser(userId, limit = 2, offset = 2)

        assertEquals(2, page1.size)
        assertEquals(2, page2.size)
        assertTrue(page1.map { it.name }.intersect(page2.map { it.name }.toSet()).isEmpty())
    }

    @Test
    fun `countSearchByUser counts only user cards`() {
        val card1 = saveCard()
        val card2 = saveCard()
        val card3 = saveCard()
        addToCollection(userId, card1.id!!)
        addToCollection(userId, card2.id!!)
        addToCollection(otherUserId, card3.id!!)

        val count = countByUser(userId)

        assertEquals(2L, count)
    }

    private fun searchByUser(
        userId: Long,
        queryText: String? = null,
        setCode: String? = null,
        rarity: String? = null,
        limit: Int = 20,
        offset: Long = 0,
    ) = cardSqlMapper.searchByUser(
        userId = userId,
        queryText = queryText,
        setCode = setCode,
        collectorNumber = null,
        colors = null,
        colorIdentity = null,
        type = null,
        rarity = rarity,
        order = CardSortOrder.NAME,
        direction = SortDirection.ASC,
        limit = limit,
        offset = offset,
    )

    private fun countByUser(userId: Long) = cardSqlMapper.countSearchByUser(
        userId = userId,
        queryText = null,
        setCode = null,
        collectorNumber = null,
        colors = null,
        colorIdentity = null,
        type = null,
        rarity = null,
    )

    private fun saveCard(name: String = "Test Card", setCode: String = "tst", rarity: String = "common"): CardRecord {
        val record = buildRecord(name = name, setCode = setCode, rarity = rarity)
        return cardSqlMapper.upsertBatch(listOf(record)).single()
    }

    private fun addToCollection(userId: Long, cardId: Long, foil: Boolean = false) {
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

    private fun buildRecord(
        name: String = "Test Card",
        setCode: String = "tst",
        rarity: String = "common",
    ): CardRecord = CardRecord(
        id = null,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = name,
        lang = "en",
        layout = "normal",
        manaCost = "{1}",
        cmc = 1.0,
        typeLine = "Instant",
        oracleText = null,
        colors = emptyList(),
        colorIdentity = emptyList(),
        keywords = emptyList(),
        power = null,
        toughness = null,
        loyalty = null,
        setCode = setCode,
        setName = "Test Set",
        collectorNumber = (++collectorCounter).toString(),
        rarity = rarity,
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
        mtgaId = null,
    )
}
