package xyz.candycrawler.collectionmanager.application.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.domain.collection.repository.CollectionEntryRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionPersistenceServiceTest {

    private val cardRepository: CardRepository = mock()
    private val collectionEntryRepository: CollectionEntryRepository = mock()

    private val service = CollectionPersistenceService(cardRepository, collectionEntryRepository)

    private val userId = 1L

    @Test
    fun `saveImportedData saves cards and creates collection entries`() {
        val card = buildCard(id = 1L, setCode = "dsk", collectorNumber = "100")
        val quantityByKey = mapOf(Triple("dsk", "100", false) to 3)

        whenever(cardRepository.saveAll(listOf(card))).thenReturn(listOf(card))

        val result = service.saveImportedData(userId, listOf(card), quantityByKey)

        assertEquals(1, result)

        val entriesCaptor = argumentCaptor<List<CollectionEntry>>()
        verify(collectionEntryRepository).saveAll(entriesCaptor.capture())

        val entry = entriesCaptor.firstValue.single()
        assertEquals(userId, entry.userId)
        assertEquals(1L, entry.cardId)
        assertEquals(3, entry.quantity)
        assertEquals(false, entry.foil)
    }

    @Test
    fun `saveImportedData skips cards not returned by cardRepository`() {
        val card = buildCard(id = 10L, setCode = "dsk", collectorNumber = "200")
        val quantityByKey = mapOf(
            Triple("dsk", "200", false) to 2,
            Triple("dsk", "999", false) to 1,
        )

        whenever(cardRepository.saveAll(listOf(card))).thenReturn(listOf(card))

        val result = service.saveImportedData(userId, listOf(card), quantityByKey)

        assertEquals(1, result)

        val entriesCaptor = argumentCaptor<List<CollectionEntry>>()
        verify(collectionEntryRepository).saveAll(entriesCaptor.capture())
        assertEquals(1, entriesCaptor.firstValue.size)
        assertEquals(10L, entriesCaptor.firstValue.single().cardId)
    }

    @Test
    fun `saveImportedData returns zero and saves empty entries when no cards`() {
        whenever(cardRepository.saveAll(emptyList())).thenReturn(emptyList())

        val result = service.saveImportedData(userId, emptyList(), emptyMap())

        assertEquals(0, result)

        val entriesCaptor = argumentCaptor<List<CollectionEntry>>()
        verify(collectionEntryRepository).saveAll(entriesCaptor.capture())
        assertTrue(entriesCaptor.firstValue.isEmpty())
    }

    @Test
    fun `saveImportedData creates one entry per unique card`() {
        val cards = listOf(
            buildCard(id = 1L, setCode = "dsk", collectorNumber = "1"),
            buildCard(id = 2L, setCode = "dsk", collectorNumber = "2"),
            buildCard(id = 3L, setCode = "dsk", collectorNumber = "3"),
        )
        val quantityByKey = mapOf(
            Triple("dsk", "1", false) to 4,
            Triple("dsk", "2", false) to 2,
            Triple("dsk", "3", false) to 1,
        )

        whenever(cardRepository.saveAll(cards)).thenReturn(cards)

        val result = service.saveImportedData(userId, cards, quantityByKey)

        assertEquals(3, result)

        val entriesCaptor = argumentCaptor<List<CollectionEntry>>()
        verify(collectionEntryRepository).saveAll(entriesCaptor.capture())

        val entries = entriesCaptor.firstValue.sortedBy { it.cardId }
        assertEquals(listOf(1L, 2L, 3L), entries.map { it.cardId })
        assertEquals(listOf(4, 2, 1), entries.map { it.quantity })
        assertTrue(entries.all { it.userId == userId })
    }

    private fun buildCard(id: Long, setCode: String, collectorNumber: String): Card = Card(
        id = id,
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
