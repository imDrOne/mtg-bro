package xyz.candycrawler.collectionmanager.application.rest

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallCardResponse
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallSearchResponse
import java.util.UUID
import kotlin.test.assertEquals

class ScryfallProxyControllerTest {

    private val scryfallApiClient: ScryfallApiClient = mock()
    private val controller = ScryfallProxyController(scryfallApiClient)

    @Test
    fun `searchCards delegates to scryfallApiClient with same params`() {
        val response = ScryfallSearchResponse(
            objectType = "list",
            totalCards = 1,
            hasMore = false,
            nextPage = null,
            data = listOf(
                ScryfallCardResponse(
                    id = UUID.randomUUID(),
                    oracleId = UUID.randomUUID(),
                    name = "Lightning Bolt",
                    lang = "en",
                    layout = "normal",
                    manaCost = "{R}",
                    cmc = 1.0,
                    typeLine = "Instant",
                    oracleText = "Deal 3 damage.",
                    colors = listOf("R"),
                    colorIdentity = listOf("R"),
                    keywords = emptyList(),
                    power = null,
                    toughness = null,
                    loyalty = null,
                    setCode = "lea",
                    setName = "Limited Edition Alpha",
                    collectorNumber = "232",
                    rarity = "common",
                    releasedAt = "1993-08-05",
                    imageUris = null,
                    prices = null,
                    flavorText = null,
                    artist = "Christopher Rush",
                ),
            ),
        )
        whenever(
            scryfallApiClient.searchCards(
                query = "bolt",
                unique = "cards",
                order = "name",
                dir = "asc",
                includeExtras = false,
                includeMultilingual = false,
                includeVariations = false,
                page = 2,
            ),
        ).thenReturn(response)

        val result = controller.searchCards(
            q = "bolt",
            unique = "cards",
            order = "name",
            dir = "asc",
            includeExtras = false,
            includeMultilingual = false,
            includeVariations = false,
            page = 2,
        )

        assertEquals(response, result)
        assertEquals(1, result.totalCards)
        assertEquals("Lightning Bolt", result.data.single().name)
        verify(scryfallApiClient).searchCards(
            query = "bolt",
            unique = "cards",
            order = "name",
            dir = "asc",
            includeExtras = false,
            includeMultilingual = false,
            includeVariations = false,
            page = 2,
        )
    }

    @Test
    fun `searchCards passes null optional params to client`() {
        whenever(
            scryfallApiClient.searchCards(
                query = "t:creature",
                unique = null,
                order = null,
                dir = null,
                includeExtras = null,
                includeMultilingual = null,
                includeVariations = null,
                page = null,
            ),
        ).thenReturn(
            ScryfallSearchResponse(
                objectType = "list",
                totalCards = 0,
                hasMore = false,
                nextPage = null,
                data = emptyList(),
            ),
        )

        controller.searchCards(
            q = "t:creature",
            unique = null,
            order = null,
            dir = null,
            includeExtras = null,
            includeMultilingual = null,
            includeVariations = null,
            page = null,
        )

        verify(scryfallApiClient).searchCards(
            query = "t:creature",
            unique = null,
            order = null,
            dir = null,
            includeExtras = null,
            includeMultilingual = null,
            includeVariations = null,
            page = null,
        )
    }
}
