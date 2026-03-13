package xyz.candycrawler.collectionmanager.application.rest

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.model.CardPage
import xyz.candycrawler.collectionmanager.domain.card.model.CardSearchCriteria
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class CardSearchControllerTest {

    private val cardRepository: CardRepository = mock()
    private val controller = CardSearchController(cardRepository)

    @Test
    fun `searchCards builds criteria from params and returns paginated response`() {
        val card = buildCard(id = 1L, name = "Lightning Bolt")
        val page = CardPage(
            cards = listOf(card),
            totalCards = 1L,
            hasMore = false,
            page = 1,
            pageSize = 20,
        )
        whenever(cardRepository.search(argThat { criteria ->
            criteria.query == "bolt" &&
                criteria.setCode == "neo" &&
                criteria.rarity == "rare" &&
                criteria.page == 1 &&
                criteria.pageSize == 20
        })).thenReturn(page)

        val response = controller.searchCards(
            q = "bolt",
            set = "neo",
            rarity = "rare",
            order = "name",
            dir = "asc",
            page = 1,
            pageSize = 20,
        )

        assertEquals(1L, response.totalCards)
        assertEquals(false, response.hasMore)
        assertEquals(1, response.page)
        assertEquals(20, response.pageSize)
        assertEquals(1, response.data.size)
        assertEquals("Lightning Bolt", response.data.single().name)
        assertEquals(1L, response.data.single().id)
    }

    @Test
    fun `searchCards coerces page to at least 1`() {
        whenever(cardRepository.search(argThat { c: CardSearchCriteria -> c.page == 1 }))
            .thenReturn(CardPage(emptyList(), 0L, false, 1, 20))

        controller.searchCards(q = null, set = null, rarity = null, order = "name", dir = "auto", page = 0, pageSize = 20)

        verify(cardRepository).search(argThat { c: CardSearchCriteria -> c.page == 1 })
    }

    @Test
    fun `searchCards coerces pageSize to max 175`() {
        whenever(cardRepository.search(argThat { c: CardSearchCriteria -> c.pageSize == 175 }))
            .thenReturn(CardPage(emptyList(), 0L, false, 1, 175))

        controller.searchCards(q = null, set = null, rarity = null, order = "name", dir = "auto", page = 1, pageSize = 500)

        verify(cardRepository).search(argThat { c: CardSearchCriteria -> c.pageSize == 175 })
    }

    @Test
    fun `searchCards maps card to response with imageUris and prices`() {
        val card = buildCard(
            id = 2L,
            name = "Test",
            imageUriSmall = "https://small",
            imageUriNormal = "https://normal",
            priceUsd = "1.50",
            priceEur = "1.20",
        )
        whenever(cardRepository.search(any()))
            .thenReturn(CardPage(listOf(card), 1L, false, 1, 20))

        val response = controller.searchCards(q = "test", set = null, rarity = null, order = "name", dir = "auto", page = 1, pageSize = 20)

        val dto = response.data.single()
        assertEquals("https://small", dto.imageUris?.small)
        assertEquals("https://normal", dto.imageUris?.normal)
        assertEquals("1.50", dto.prices?.usd)
        assertEquals("1.20", dto.prices?.eur)
    }

    private fun buildCard(
        id: Long? = null,
        name: String = "Card",
        imageUriSmall: String? = null,
        imageUriNormal: String? = null,
        priceUsd: String? = null,
        priceEur: String? = null,
    ): Card = Card(
        id = id,
        scryfallId = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = name,
        lang = "en",
        layout = "normal",
        manaCost = "{R}",
        cmc = 1.0,
        typeLine = "Instant",
        oracleText = null,
        colors = listOf("R"),
        colorIdentity = listOf("R"),
        keywords = emptyList(),
        power = null,
        toughness = null,
        loyalty = null,
        setCode = "neo",
        setName = "Neon Dynasty",
        collectorNumber = "1",
        rarity = "common",
        releasedAt = LocalDate.of(2022, 2, 18),
        imageUriSmall = imageUriSmall,
        imageUriNormal = imageUriNormal,
        imageUriLarge = null,
        imageUriPng = null,
        imageUriArtCrop = null,
        imageUriBorderCrop = null,
        priceUsd = priceUsd,
        priceUsdFoil = null,
        priceEur = priceEur,
        priceEurFoil = null,
        flavorText = null,
        artist = null,
    )
}
