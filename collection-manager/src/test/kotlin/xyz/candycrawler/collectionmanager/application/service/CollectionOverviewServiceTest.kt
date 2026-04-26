package xyz.candycrawler.collectionmanager.application.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.QueryCardRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionOverviewServiceTest {

    private val queryCardRepository: QueryCardRepository = mock()
    private val service = CollectionOverviewService(queryCardRepository)

    private val userId = 1L

    @Test
    fun `total_cards equals number of distinct cards in collection`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(buildCard(), buildCard(), buildCard()),
        )

        val overview = service.getOverview(userId)

        assertEquals(3, overview.totalCards)
    }

    @Test
    fun `empty collection returns all-zero overview`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(emptyList())

        val overview = service.getOverview(userId)

        assertEquals(0, overview.totalCards)
        assertTrue(overview.byColor.isEmpty())
        assertTrue(overview.byType.isEmpty())
        assertTrue(overview.topTribes.isEmpty())
        assertTrue(overview.byRarity.isEmpty())
    }

    @Test
    fun `by_color counts each individual color independently`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(colors = listOf("U")),
                buildCard(colors = listOf("U")),
                buildCard(colors = listOf("W", "U")),
                buildCard(colors = listOf("R")),
            ),
        )

        val overview = service.getOverview(userId)

        assertEquals(1, overview.byColor["W"])
        assertEquals(3, overview.byColor["U"])
        assertEquals(1, overview.byColor["R"])
    }

    @Test
    fun `colorless cards are counted under C`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(colors = emptyList()),
                buildCard(colors = emptyList()),
            ),
        )

        val overview = service.getOverview(userId)

        assertEquals(2, overview.byColor["C"])
    }

    @Test
    fun `by_type identifies creature type correctly`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk"),
                buildCard(typeLine = "Legendary Creature — Human Wizard"),
                buildCard(typeLine = "Instant"),
                buildCard(typeLine = "Sorcery"),
                buildCard(typeLine = "Enchantment"),
                buildCard(typeLine = "Artifact"),
                buildCard(typeLine = "Land"),
            ),
        )

        val overview = service.getOverview(userId)

        assertEquals(2, overview.byType["creature"])
        assertEquals(1, overview.byType["instant"])
        assertEquals(1, overview.byType["sorcery"])
        assertEquals(1, overview.byType["enchantment"])
        assertEquals(1, overview.byType["artifact"])
        assertEquals(1, overview.byType["land"])
    }

    @Test
    fun `artifact creature counts as creature not artifact`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(buildCard(typeLine = "Artifact Creature — Construct")),
        )

        val overview = service.getOverview(userId)

        assertEquals(1, overview.byType["creature"])
        assertEquals(null, overview.byType["artifact"])
    }

    @Test
    fun `by_rarity groups cards correctly`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(rarity = "mythic"),
                buildCard(rarity = "rare"),
                buildCard(rarity = "rare"),
                buildCard(rarity = "common"),
            ),
        )

        val overview = service.getOverview(userId)

        assertEquals(1, overview.byRarity["mythic"])
        assertEquals(2, overview.byRarity["rare"])
        assertEquals(1, overview.byRarity["common"])
    }

    @Test
    fun `top_tribes extracts subtypes from creature type lines`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk Wizard"),
                buildCard(typeLine = "Creature — Merfolk"),
                buildCard(typeLine = "Creature — Elf"),
                buildCard(typeLine = "Instant"),
            ),
        )

        val overview = service.getOverview(userId)

        val merfolk = overview.topTribes.find { it.name == "Merfolk" }
        val elf = overview.topTribes.find { it.name == "Elf" }
        val wizard = overview.topTribes.find { it.name == "Wizard" }
        assertEquals(2, merfolk?.count)
        assertEquals(1, elf?.count)
        assertEquals(1, wizard?.count)
    }

    @Test
    fun `top_tribes returns at most 10 entries sorted by count descending`() {
        val cards = (1..15).map { i -> buildCard(typeLine = "Creature — Tribe$i") } +
            (1..5).map { buildCard(typeLine = "Creature — Tribe1") }
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(cards)

        val overview = service.getOverview(userId)

        assertEquals(10, overview.topTribes.size)
        assertEquals("Tribe1", overview.topTribes.first().name)
        assertEquals(6, overview.topTribes.first().count)
    }

    @Test
    fun `top_tribes colors are union of individual card colors sorted WUBRG`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("U")),
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("W", "U")),
                buildCard(typeLine = "Creature — Merfolk", colors = listOf("U")),
            ),
        )

        val overview = service.getOverview(userId)

        val merfolk = overview.topTribes.find { it.name == "Merfolk" }
        assertEquals("WU", merfolk?.colors)
    }

    @Test
    fun `top_tribes excludes non-creature cards`() {
        whenever(queryCardRepository.findAllInCollection(userId)).thenReturn(
            listOf(
                buildCard(typeLine = "Instant"),
                buildCard(typeLine = "Kindred Instant — Merfolk"),
                buildCard(typeLine = "Creature — Merfolk"),
            ),
        )

        val overview = service.getOverview(userId)

        val merfolk = overview.topTribes.find { it.name == "Merfolk" }
        assertEquals(1, merfolk?.count)
    }

    private fun buildCard(
        typeLine: String = "Creature — Merfolk",
        colors: List<String> = listOf("U"),
        rarity: String = "common",
    ): Card = Card(
        id = null,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Test Card",
        lang = "en",
        layout = "normal",
        manaCost = "{U}",
        cmc = 2.0,
        typeLine = typeLine,
        oracleText = null,
        colors = colors,
        colorIdentity = colors,
        keywords = emptyList(),
        power = "1",
        toughness = "1",
        loyalty = null,
        setCode = "tst",
        setName = "Test Set",
        collectorNumber = UUID.randomUUID().toString().take(3),
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
    )
}
