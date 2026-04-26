package xyz.candycrawler.collectionmanager.application.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.tribal.exception.InvalidTribalQueryException
import xyz.candycrawler.collectionmanager.infrastructure.cache.CreatureTypeCacheService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TribalAnalysisServiceTest {

    private val cardRepository: CardRepository = mock()
    private val creatureTypeCacheService: CreatureTypeCacheService = mock()

    private val service = TribalAnalysisService(cardRepository, creatureTypeCacheService)

    private val userId = 1L

    @Test
    fun `unknown tribe throws InvalidTribalQueryException`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk", "Elf"))

        assertFailsWith<InvalidTribalQueryException> {
            service.analyze(userId, "Mermaid")
        }
    }

    @Test
    fun `tribe validation is case-insensitive`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "merfolk")).thenReturn(emptyList())

        val stats = service.analyze(userId, "merfolk")

        assertEquals("merfolk", stats.tribe)
    }

    @Test
    fun `empty collection returns all-zero stats with weak viability`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(emptyList())

        val stats = service.analyze(userId, "Merfolk")

        assertEquals(0, stats.totalCards)
        assertEquals(0, stats.creatures)
        assertEquals(0, stats.tribalSpells)
        assertEquals(0, stats.tribalSupport)
        assertTrue(stats.byCmc.isEmpty())
        assertTrue(stats.colorSpread.isEmpty())
        assertFalse(stats.hasLord)
        assertFalse(stats.hasCommander)
        assertEquals("weak", stats.deckViability)
    }

    @Test
    fun `creatures kindred spells and tribal support are counted correctly`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk Wizard", oracleText = "Flying."),
                buildCard(typeLine = "Creature — Merfolk", oracleText = null),
                buildCard(typeLine = "Kindred Instant — Merfolk", oracleText = "Counter target spell."),
                buildCard(typeLine = "Kindred Sorcery — Merfolk", oracleText = "Draw a card."),
                buildCard(typeLine = "Instant", oracleText = "All Merfolk you control get +1/+1."),
                buildCard(typeLine = "Enchantment", oracleText = "Merfolk creatures have hexproof."),
            ),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertEquals(6, stats.totalCards)
        assertEquals(2, stats.creatures)
        assertEquals(2, stats.tribalSpells)
        assertEquals(2, stats.tribalSupport)
    }

    @Test
    fun `artifact creature with tribe subtype counts as creature`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(buildCard(typeLine = "Artifact Creature — Merfolk Construct")),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertEquals(1, stats.creatures)
        assertEquals(0, stats.tribalSpells)
        assertEquals(0, stats.tribalSupport)
    }

    @Test
    fun `by_cmc groups correctly with 5plus bucket`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Elf"))
        whenever(cardRepository.findByTribe(userId, "Elf")).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Elf", cmc = 1.0),
                buildCard(typeLine = "Creature — Elf", cmc = 2.0),
                buildCard(typeLine = "Creature — Elf", cmc = 2.0),
                buildCard(typeLine = "Creature — Elf", cmc = 4.0),
                buildCard(typeLine = "Creature — Elf", cmc = 5.0),
                buildCard(typeLine = "Creature — Elf", cmc = 7.0),
            ),
        )

        val stats = service.analyze(userId, "Elf")

        assertEquals(mapOf("1" to 1, "2" to 2, "4" to 1, "5+" to 2), stats.byCmc)
    }

    @Test
    fun `color_spread groups by sorted WUBRG combination`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("U")),
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("U")),
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("W", "U")),
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("U", "W")), // same combo, different order
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("R")),
            ),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertEquals(mapOf("U" to 2, "WU" to 2, "R" to 1), stats.colorSpread)
    }

    @Test
    fun `has_commander is true when legendary creature of tribe exists`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(buildCard(typeLine = "Legendary Creature — Merfolk Noble")),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertTrue(stats.hasCommander)
    }

    @Test
    fun `has_commander is false when no legendary creature of tribe exists`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(buildCard(typeLine = "Creature — Merfolk Wizard")),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertFalse(stats.hasCommander)
    }

    @Test
    fun `has_lord is true when any legendary tribal card exists`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk"),
                buildCard(typeLine = "Legendary Creature — Merfolk Noble"),
            ),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertTrue(stats.hasLord)
    }

    @Test
    fun `has_lord is false when no legendary tribal card exists`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Merfolk"))
        whenever(cardRepository.findByTribe(userId, "Merfolk")).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk"),
                buildCard(typeLine = "Kindred Instant — Merfolk"),
            ),
        )

        val stats = service.analyze(userId, "Merfolk")

        assertFalse(stats.hasLord)
    }

    @Test
    fun `deck_viability is strong with 20 or more creatures`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Goblin"))
        whenever(cardRepository.findByTribe(userId, "Goblin")).thenReturn(
            (1..20).map { buildCard(typeLine = "Creature — Goblin") },
        )

        val stats = service.analyze(userId, "Goblin")

        assertEquals("strong", stats.deckViability)
    }

    @Test
    fun `deck_viability is moderate with 10 to 19 creatures`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Goblin"))
        whenever(cardRepository.findByTribe(userId, "Goblin")).thenReturn(
            (1..15).map { buildCard(typeLine = "Creature — Goblin") },
        )

        val stats = service.analyze(userId, "Goblin")

        assertEquals("moderate", stats.deckViability)
    }

    @Test
    fun `deck_viability is weak with fewer than 10 creatures`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Goblin"))
        whenever(cardRepository.findByTribe(userId, "Goblin")).thenReturn(
            (1..9).map { buildCard(typeLine = "Creature — Goblin") },
        )

        val stats = service.analyze(userId, "Goblin")

        assertEquals("weak", stats.deckViability)
    }

    @Test
    fun `non-creature tribal cards do not count toward deck viability`() {
        whenever(creatureTypeCacheService.getCreatureTypes()).thenReturn(setOf("Elf"))
        whenever(cardRepository.findByTribe(userId, "Elf")).thenReturn(
            listOf(
                buildCard(typeLine = "Kindred Sorcery — Elf"),
                buildCard(typeLine = "Kindred Enchantment — Elf"),
                buildCard(typeLine = "Instant", oracleText = "All Elf creatures get +2/+2."),
            ),
        )

        val stats = service.analyze(userId, "Elf")

        assertEquals("weak", stats.deckViability)
    }

    private fun buildCard(
        typeLine: String = "Creature — Merfolk",
        oracleText: String? = null,
        colors: List<String> = listOf("U"),
        cmc: Double = 2.0,
    ): Card = Card(
        id = null,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Test Card",
        lang = "en",
        layout = "normal",
        manaCost = "{U}",
        cmc = cmc,
        typeLine = typeLine,
        oracleText = oracleText,
        colors = colors,
        colorIdentity = colors,
        keywords = emptyList(),
        power = "1",
        toughness = "1",
        loyalty = null,
        setCode = "tst",
        setName = "Test Set",
        collectorNumber = UUID.randomUUID().toString().take(3),
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
