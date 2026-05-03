package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.model.CardSortOrder
import xyz.candycrawler.collectionmanager.domain.card.model.SortDirection
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper
import xyz.candycrawler.collectionmanager.lib.AbstractIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Transactional
class CardSqlMapperTest(@Autowired private val sqlMapper: CardSqlMapper) : AbstractIntegrationTest() {

    @Test
    fun `upsertBatch persists all fields correctly`() {
        val record = buildRecord(setCode = "dsk", collectorNumber = "100")

        val saved = sqlMapper.upsertBatch(listOf(record))

        val result = sqlMapper.selectBySetCodeAndCollectorNumber("dsk", "100")
        assertNotNull(result)
        assertNotNull(result.id)
        assertEquals(saved.single().id, result.id)
        assertEquals(record.scryfallId, result.scryfallId)
        assertEquals(record.oracleId, result.oracleId)
        assertEquals(record.name, result.name)
        assertEquals(record.lang, result.lang)
        assertEquals(record.layout, result.layout)
        assertEquals(record.manaCost, result.manaCost)
        assertEquals(record.cmc, result.cmc)
        assertEquals(record.typeLine, result.typeLine)
        assertEquals(record.oracleText, result.oracleText)
        assertEquals(record.colors, result.colors)
        assertEquals(record.colorIdentity, result.colorIdentity)
        assertEquals(record.keywords, result.keywords)
        assertEquals(record.power, result.power)
        assertEquals(record.toughness, result.toughness)
        assertEquals(record.setCode, result.setCode)
        assertEquals(record.setName, result.setName)
        assertEquals(record.collectorNumber, result.collectorNumber)
        assertEquals(record.rarity, result.rarity)
        assertEquals(record.releasedAt, result.releasedAt)
        assertEquals(record.imageUriNormal, result.imageUriNormal)
        assertEquals(record.priceUsd, result.priceUsd)
        assertEquals(record.artist, result.artist)
    }

    @Test
    fun `upsertBatch persists multiple records and returns all with ids`() {
        val records = listOf(
            buildRecord(setCode = "dsk", collectorNumber = "1"),
            buildRecord(setCode = "dsk", collectorNumber = "2"),
            buildRecord(setCode = "dsk", collectorNumber = "3"),
        )

        val saved = sqlMapper.upsertBatch(records)

        assertEquals(3, saved.size)
        saved.forEach { assertNotNull(it.id) }
    }

    @Test
    fun `upsertBatch on same unique key updates the record`() {
        val original = buildRecord(setCode = "dsk", collectorNumber = "200", name = "Original Name")
        val updated = buildRecord(setCode = "dsk", collectorNumber = "200", name = "Updated Name")

        sqlMapper.upsertBatch(listOf(original))
        sqlMapper.upsertBatch(listOf(updated))

        val result = sqlMapper.selectBySetCodeAndCollectorNumber("dsk", "200")
        assertNotNull(result)
        assertEquals("Updated Name", result.name)
    }

    @Test
    fun `upsertBatch persists null nullable fields`() {
        val record = buildRecord(
            setCode = "dsk",
            collectorNumber = "300",
            manaCost = null,
            oracleText = null,
            power = null,
            toughness = null,
            releasedAt = null,
            imageUriNormal = null,
            priceUsd = null,
            flavorText = null,
            artist = null,
        )

        sqlMapper.upsertBatch(listOf(record))

        val result = sqlMapper.selectBySetCodeAndCollectorNumber("dsk", "300")
        assertNotNull(result)
        assertNull(result.manaCost)
        assertNull(result.oracleText)
        assertNull(result.power)
        assertNull(result.toughness)
        assertNull(result.releasedAt)
        assertNull(result.imageUriNormal)
        assertNull(result.priceUsd)
        assertNull(result.flavorText)
        assertNull(result.artist)
    }

    @Test
    fun `selectById returns record when found`() {
        val saved = sqlMapper.upsertBatch(listOf(buildRecord(setCode = "dsk", collectorNumber = "400"))).single()

        val result = sqlMapper.selectById(saved.id!!)

        assertNotNull(result)
        assertEquals(saved.id, result.id)
        assertEquals("dsk", result.setCode)
        assertEquals("400", result.collectorNumber)
    }

    @Test
    fun `selectById returns null when not found`() {
        val result = sqlMapper.selectById(Long.MAX_VALUE)

        assertNull(result)
    }

    @Test
    fun `selectBySetCodeAndCollectorNumber returns null when not found`() {
        val result = sqlMapper.selectBySetCodeAndCollectorNumber("xxx", "999")

        assertNull(result)
    }

    @Test
    fun `selectBySetCodeAndCollectorNumber returns null when set code does not match`() {
        sqlMapper.upsertBatch(listOf(buildRecord(setCode = "dsk", collectorNumber = "500")))

        val result = sqlMapper.selectBySetCodeAndCollectorNumber("ecl", "500")

        assertNull(result)
    }

    @Test
    fun `search returns records matching query in name`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "neo", collectorNumber = "1", name = "Burning Sun"),
                buildRecord(setCode = "neo", collectorNumber = "2", name = "Cold Moon"),
                buildRecord(setCode = "neo", collectorNumber = "3", name = "Sunrise"),
            ),
        )

        val result = sqlMapper.search(
            queryText = "sun",
            setCode = null,
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Burning Sun" })
        assertTrue(result.any { it.name == "Sunrise" })
    }

    @Test
    fun `search returns records matching query in type line`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "dmu", collectorNumber = "1", typeLine = "Creature — Human"),
                buildRecord(setCode = "dmu", collectorNumber = "2", typeLine = "Instant"),
                buildRecord(setCode = "dmu", collectorNumber = "3", typeLine = "Creature — Elf"),
            ),
        )

        val result = sqlMapper.search(
            queryText = "instant",
            setCode = null,
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(1, result.size)
        assertEquals("Instant", result.single().typeLine)
    }

    @Test
    fun `search returns records matching query in oracle text`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "bro", collectorNumber = "1", oracleText = "Draw two cards."),
                buildRecord(setCode = "bro", collectorNumber = "2", oracleText = "Flying. Lifelink."),
                buildRecord(setCode = "bro", collectorNumber = "3", oracleText = "Draw a card."),
            ),
        )

        val result = sqlMapper.search(
            queryText = "lifelink",
            setCode = null,
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(1, result.size)
        assertTrue(result.single().oracleText!!.contains("Lifelink"))
    }

    @Test
    fun `search filters by set code`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "neo", collectorNumber = "1", name = "Card A"),
                buildRecord(setCode = "dmu", collectorNumber = "1", name = "Card B"),
                buildRecord(setCode = "neo", collectorNumber = "2", name = "Card C"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = "neo",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.setCode == "neo" })
    }

    @Test
    fun `search filters by colors`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "x", collectorNumber = "1", name = "A", colors = listOf("W", "U")),
                buildRecord(setCode = "x", collectorNumber = "2", name = "B", colors = listOf("R")),
                buildRecord(setCode = "x", collectorNumber = "3", name = "C", colors = listOf("W", "U", "B")),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = "x",
            collectorNumber = null,
            colors = listOf("W", "U"),
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "A" })
        assertTrue(result.any { it.name == "C" })
        assert(!result.any { it.name == "B" })
    }

    @Test
    fun `search filters by type`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "x", collectorNumber = "1", name = "Creature A", typeLine = "Creature — Human"),
                buildRecord(setCode = "x", collectorNumber = "2", name = "Instant B", typeLine = "Instant"),
                buildRecord(setCode = "x", collectorNumber = "3", name = "Land C", typeLine = "Basic Land — Forest"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = "x",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = "creature",
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(1, result.size)
        assertEquals("Creature A", result.single().name)
    }

    @Test
    fun `search filters by collector number`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "neo", collectorNumber = "100", name = "Card A"),
                buildRecord(setCode = "neo", collectorNumber = "101", name = "Card B"),
                buildRecord(setCode = "neo", collectorNumber = "102", name = "Card C"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = "neo",
            collectorNumber = "101",
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(1, result.size)
        assertEquals("101", result.single().collectorNumber)
        assertEquals("Card B", result.single().name)
    }

    @Test
    fun `search filters by rarity`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "x", collectorNumber = "1", name = "C1", rarity = "common"),
                buildRecord(setCode = "x", collectorNumber = "2", name = "C2", rarity = "rare"),
                buildRecord(setCode = "x", collectorNumber = "3", name = "C3", rarity = "common"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = null,
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = "rare",
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(1, result.size)
        assertEquals("rare", result.single().rarity)
    }

    @Test
    fun `search applies limit and offset`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "lim", collectorNumber = "1", name = "Card 1"),
                buildRecord(setCode = "lim", collectorNumber = "2", name = "Card 2"),
                buildRecord(setCode = "lim", collectorNumber = "3", name = "Card 3"),
                buildRecord(setCode = "lim", collectorNumber = "4", name = "Card 4"),
                buildRecord(setCode = "lim", collectorNumber = "5", name = "Card 5"),
            ),
        )

        val page1 = sqlMapper.search(
            queryText = null,
            setCode = "lim",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 2,
            offset = 0L,
        )
        val page2 = sqlMapper.search(
            queryText = null,
            setCode = "lim",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 2,
            offset = 2L,
        )

        assertEquals(2, page1.size)
        assertEquals(2, page2.size)
        assertEquals("Card 1", page1.first().name)
        assertEquals("Card 3", page2.first().name)
    }

    @Test
    fun `search orders by name ascending`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "ord", collectorNumber = "1", name = "Zebra"),
                buildRecord(setCode = "ord", collectorNumber = "2", name = "Alpha"),
                buildRecord(setCode = "ord", collectorNumber = "3", name = "Mono"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = "ord",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(listOf("Alpha", "Mono", "Zebra"), result.map { it.name })
    }

    @Test
    fun `search orders by name descending`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "ord2", collectorNumber = "1", name = "A"),
                buildRecord(setCode = "ord2", collectorNumber = "2", name = "B"),
                buildRecord(setCode = "ord2", collectorNumber = "3", name = "C"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = "ord2",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.DESC,
            limit = 10,
            offset = 0L,
        )

        assertEquals(listOf("C", "B", "A"), result.map { it.name })
    }

    @Test
    fun `countSearch returns total count without pagination`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "cnt", collectorNumber = "1", name = "One"),
                buildRecord(setCode = "cnt", collectorNumber = "2", name = "Two"),
                buildRecord(setCode = "cnt", collectorNumber = "3", name = "Three"),
            ),
        )

        val total = sqlMapper.countSearch(
            queryText = null,
            setCode = "cnt",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
        )

        assertEquals(3L, total)
    }

    @Test
    fun `countSearch returns count matching query`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "cnt2", collectorNumber = "1", name = "Lightning Bolt"),
                buildRecord(setCode = "cnt2", collectorNumber = "2", name = "Lightning Helix"),
                buildRecord(setCode = "cnt2", collectorNumber = "3", name = "Shock"),
            ),
        )

        val total = sqlMapper.countSearch(
            queryText = "lightning",
            setCode = "cnt2",
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
        )

        assertEquals(2L, total)
    }

    @Test
    fun `search with no criteria returns all records ordered`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(setCode = "all", collectorNumber = "1", name = "First"),
                buildRecord(setCode = "all", collectorNumber = "2", name = "Second"),
            ),
        )

        val result = sqlMapper.search(
            queryText = null,
            setCode = null,
            collectorNumber = null,
            colors = null,
            colorIdentity = null,
            type = null,
            rarity = null,
            order = CardSortOrder.NAME,
            direction = SortDirection.ASC,
            limit = 10,
            offset = 0L,
        )

        assertTrue(result.size >= 2)
        val names = result.map { it.name }
        assertEquals(names.sorted(), names)
    }

    private fun buildRecord(
        setCode: String = "dsk",
        collectorNumber: String = "1",
        name: String = "Eclipsed Elf",
        typeLine: String = "Creature — Elf",
        manaCost: String? = "{G}",
        oracleText: String? = "When Eclipsed Elf enters, draw a card.",
        rarity: String = "common",
        power: String? = "1",
        toughness: String? = "1",
        releasedAt: LocalDate? = LocalDate.of(2024, 9, 27),
        imageUriNormal: String? = "https://cards.scryfall.io/normal/front/test.jpg",
        priceUsd: String? = "0.10",
        flavorText: String? = "A whisper in the dark.",
        artist: String? = "Some Artist",
        colors: List<String> = listOf("G"),
        colorIdentity: List<String> = listOf("G"),
    ): CardRecord = CardRecord(
        id = null,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = name,
        lang = "en",
        layout = "normal",
        manaCost = manaCost,
        cmc = 1.0,
        typeLine = typeLine,
        oracleText = oracleText,
        colors = colors,
        colorIdentity = colorIdentity,
        keywords = emptyList(),
        power = power,
        toughness = toughness,
        loyalty = null,
        setCode = setCode,
        setName = "Duskmourn: House of Horror",
        collectorNumber = collectorNumber,
        rarity = rarity,
        releasedAt = releasedAt,
        imageUriSmall = null,
        imageUriNormal = imageUriNormal,
        imageUriLarge = null,
        imageUriPng = null,
        imageUriArtCrop = null,
        imageUriBorderCrop = null,
        priceUsd = priceUsd,
        priceUsdFoil = null,
        priceEur = null,
        priceEurFoil = null,
        flavorText = flavorText,
        artist = artist,
    )
}
