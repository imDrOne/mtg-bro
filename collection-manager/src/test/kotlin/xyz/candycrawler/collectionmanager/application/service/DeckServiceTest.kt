package xyz.candycrawler.collectionmanager.application.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.card.exception.CardNotFoundException
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.deck.model.Deck
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckEntry
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckFormat
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckHeader
import xyz.candycrawler.collectionmanager.domain.deck.repository.DeckRepository
import xyz.candycrawler.collectionmanager.domain.deck.repository.QueryDeckRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeckServiceTest {

    private val cardRepository: CardRepository = mock()
    private val deckRepository: DeckRepository = mock()
    private val queryDeckRepository: QueryDeckRepository = mock()
    private val service = DeckService(cardRepository, deckRepository, queryDeckRepository)

    private val userId = 1L

    @Test
    fun `save delegates to cardRepository and deckRepository`() {
        val cards = (1..60).map { buildCard(id = it.toLong(), colorIdentity = listOf("G")) }
        whenever(cardRepository.findBySetAndCollectorPairs(any())).thenReturn(cards)

        val mainboard = cards.map { Triple(it.setCode, it.collectorNumber, 1) }
        val savedDeck = buildDeck(id = 1L)
        whenever(deckRepository.save(any())).thenReturn(savedDeck)

        val result = service.save(userId, "My Deck", DeckFormat.STANDARD, null, mainboard, emptyList())

        assertEquals(1L, result.id)
    }

    @Test
    fun `save throws CardNotFoundException when card not found`() {
        whenever(cardRepository.findBySetAndCollectorPairs(any())).thenReturn(emptyList())

        val mainboard = (1..60).map { Triple("tst", it.toString(), 1) }

        assertFailsWith<CardNotFoundException> {
            service.save(userId, "Deck", DeckFormat.STANDARD, null, mainboard, emptyList())
        }
    }

    @Test
    fun `save computes colorIdentity from mainboard cards only`() {
        val mainboard = listOf(
            buildCard(id = 1L, colorIdentity = listOf("W")),
            buildCard(id = 2L, colorIdentity = listOf("U")),
        ) + (3..60).map { buildCard(id = it.toLong(), colorIdentity = listOf("G")) }
        val sideboard = listOf(buildCard(id = 61L, colorIdentity = listOf("R")))

        whenever(cardRepository.findBySetAndCollectorPairs(any())).thenReturn(mainboard + sideboard)

        val capturedDeck = mutableListOf<Deck>()
        whenever(deckRepository.save(any())).thenAnswer { inv ->
            val deck = inv.getArgument<Deck>(0)
            capturedDeck.add(deck)
            buildDeck(id = 1L)
        }

        service.save(
            userId, "Deck", DeckFormat.STANDARD, null,
            mainboard.map { Triple(it.setCode, it.collectorNumber, 1) },
            listOf(Triple(sideboard.first().setCode, sideboard.first().collectorNumber, 1)),
        )

        val colorIdentity = capturedDeck.single().colorIdentity
        assert(colorIdentity.containsAll(listOf("G", "U", "W"))) { "Expected G, U, W but got $colorIdentity" }
        assert("R" !in colorIdentity) { "Sideboard colors should not be in colorIdentity" }
    }

    @Test
    fun `findAll delegates to queryDeckRepository`() {
        whenever(queryDeckRepository.findHeaders(userId)).thenReturn(listOf(buildDeckHeader(id = 1L), buildDeckHeader(id = 2L)))

        val result = service.findAll(userId)

        assertEquals(2, result.size)
    }

    @Test
    fun `findById delegates to deckRepository findByIdAndUser`() {
        whenever(deckRepository.findByIdAndUser(42L, userId)).thenReturn(buildDeck(id = 42L))

        val result = service.findById(userId, 42L)

        assertEquals(42L, result.id)
    }

    private fun buildCard(id: Long, colorIdentity: List<String> = listOf("G")): Card = Card(
        id = id,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Test Card $id",
        lang = "en",
        layout = "normal",
        manaCost = "{G}",
        cmc = 1.0,
        typeLine = "Creature — Elf",
        oracleText = null,
        colors = listOf("G"),
        colorIdentity = colorIdentity,
        keywords = emptyList(),
        power = "1",
        toughness = "1",
        loyalty = null,
        setCode = "tst",
        setName = "Test Set",
        collectorNumber = id.toString(),
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

    private fun buildDeckHeader(id: Long): DeckHeader = DeckHeader(
        id = id,
        name = "Test Deck",
        format = DeckFormat.STANDARD,
        colorIdentity = listOf("G"),
        comment = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    private fun buildDeck(id: Long): Deck = Deck(
        id = id,
        userId = userId,
        name = "Test Deck",
        format = DeckFormat.STANDARD,
        colorIdentity = listOf("G"),
        comment = null,
        entries = (1..60).map { DeckEntry(cardId = it.toLong(), quantity = 1) },
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )
}
