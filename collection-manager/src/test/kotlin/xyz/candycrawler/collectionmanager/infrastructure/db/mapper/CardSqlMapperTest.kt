package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import xyz.candycrawler.collectionmanager.lib.AbstractIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Transactional
class CardSqlMapperTest(
    @Autowired private val sqlMapper: CardSqlMapper,
) : AbstractIntegrationTest() {

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

    private fun buildRecord(
        setCode: String = "dsk",
        collectorNumber: String = "1",
        name: String = "Eclipsed Elf",
        manaCost: String? = "{G}",
        oracleText: String? = "When Eclipsed Elf enters, draw a card.",
        power: String? = "1",
        toughness: String? = "1",
        releasedAt: LocalDate? = LocalDate.of(2024, 9, 27),
        imageUriNormal: String? = "https://cards.scryfall.io/normal/front/test.jpg",
        priceUsd: String? = "0.10",
        flavorText: String? = "A whisper in the dark.",
        artist: String? = "Some Artist",
    ): CardRecord = CardRecord(
        id = null,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = name,
        lang = "en",
        layout = "normal",
        manaCost = manaCost,
        cmc = 1.0,
        typeLine = "Creature — Elf",
        oracleText = oracleText,
        colors = listOf("G"),
        colorIdentity = listOf("G"),
        keywords = emptyList(),
        power = power,
        toughness = toughness,
        loyalty = null,
        setCode = setCode,
        setName = "Duskmourn: House of Horror",
        collectorNumber = collectorNumber,
        rarity = "common",
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
