package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.card.exception.CardNotFoundException
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardRecordToCardMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardToCardRecordMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExposedCardRepositoryTest {

    private val sqlMapper: CardSqlMapper = mock()
    private val toRecord: CardToCardRecordMapper = CardToCardRecordMapper()
    private val toDomain: CardRecordToCardMapper = CardRecordToCardMapper()

    private val repository = ExposedCardRepository(sqlMapper, toRecord, toDomain)

    @Test
    fun `saveAll converts domains to records, calls upsertBatch, returns domains`() {
        val card = buildCard(setCode = "dsk", collectorNumber = "1")
        val record = toRecord.apply(card)
        val savedRecord = record.copy(id = 42L)

        whenever(sqlMapper.upsertBatch(listOf(record))).thenReturn(listOf(savedRecord))

        val result = repository.saveAll(listOf(card))

        verify(sqlMapper).upsertBatch(listOf(record))
        assertEquals(1, result.size)
        assertEquals(42L, result.single().id)
        assertEquals("dsk", result.single().setCode)
        assertEquals("1", result.single().collectorNumber)
    }

    @Test
    fun `saveAll returns empty list when given empty input`() {
        whenever(sqlMapper.upsertBatch(emptyList())).thenReturn(emptyList())

        val result = repository.saveAll(emptyList())

        assertEquals(emptyList(), result)
    }

    @Test
    fun `findById returns domain when record found`() {
        val record = buildRecord(id = 10L, setCode = "dsk", collectorNumber = "2")
        whenever(sqlMapper.selectById(10L)).thenReturn(record)

        val result = repository.findById(10L)

        assertEquals(10L, result.id)
        assertEquals("dsk", result.setCode)
        assertEquals("2", result.collectorNumber)
    }

    @Test
    fun `findById throws CardNotFoundException when record not found`() {
        whenever(sqlMapper.selectById(99L)).thenReturn(null)

        assertThrows<CardNotFoundException> {
            repository.findById(99L)
        }
    }

    @Test
    fun `findBySetCodeAndCollectorNumber returns domain when found`() {
        val record = buildRecord(id = 20L, setCode = "ecl", collectorNumber = "218")
        whenever(sqlMapper.selectBySetCodeAndCollectorNumber("ecl", "218")).thenReturn(record)

        val result = repository.findBySetCodeAndCollectorNumber("ecl", "218")

        assertEquals(20L, result?.id)
        assertEquals("ecl", result?.setCode)
        assertEquals("218", result?.collectorNumber)
    }

    @Test
    fun `findBySetCodeAndCollectorNumber returns null when not found`() {
        whenever(sqlMapper.selectBySetCodeAndCollectorNumber("xxx", "000")).thenReturn(null)

        val result = repository.findBySetCodeAndCollectorNumber("xxx", "000")

        assertNull(result)
    }

    private fun buildCard(
        id: Long? = null,
        setCode: String = "dsk",
        collectorNumber: String = "1",
    ): Card = Card(
        id = id,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Eclipsed Elf",
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
        setName = "Duskmourn: House of Horror",
        collectorNumber = collectorNumber,
        rarity = "common",
        releasedAt = LocalDate.of(2024, 9, 27),
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

    private fun buildRecord(
        id: Long? = null,
        setCode: String = "dsk",
        collectorNumber: String = "1",
    ): CardRecord = CardRecord(
        id = id,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Eclipsed Elf",
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
        setName = "Duskmourn: House of Horror",
        collectorNumber = collectorNumber,
        rarity = "common",
        releasedAt = LocalDate.of(2024, 9, 27),
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
