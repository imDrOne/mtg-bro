package xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.mapper

import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.dto.response.CardStatsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardStatsResponseMapperTest {

    private val mapper = CardStatsResponseMapper()

    @Test
    fun `toDomain calculates 17lands grade from GIH win rate distribution`() {
        val responses = (1..14).map { index ->
            response(name = "Card $index", mtgaId = index, everDrawnWinRate = 0.50)
        } + response(name = "Premium Common", mtgaId = 100, everDrawnWinRate = 0.70)

        val result = mapper.toDomain(responses, setCode = "DMU", matchType = "QuickDraft")

        assertEquals("A+", result.single { it.mtgaId == 100 }.tier)
    }

    @Test
    fun `toDomain skips sparse stats with null win rate`() {
        val result = mapper.toDomain(
            response(winRate = null),
            setCode = "DMU",
            matchType = "QuickDraft",
        )

        assertNull(result)
    }

    private fun response(
        name: String = "Lightning Bolt",
        mtgaId: Int = 1,
        winRate: Double? = 0.5,
        everDrawnWinRate: Double? = 0.5,
    ): CardStatsResponse = CardStatsResponse(
        name = name,
        mtgaId = mtgaId,
        color = "R",
        rarity = "common",
        url = "https://example.com/card/1",
        urlBack = "",
        types = listOf("Instant"),
        layout = "normal",
        seenCount = 10,
        avgSeen = 1.0,
        pickCount = 5,
        avgPick = 2.0,
        gameCount = 3,
        poolCount = 4,
        playRate = 0.5,
        winRate = winRate,
        openingHandGameCount = 1,
        openingHandWinRate = 0.5,
        drawnGameCount = 1,
        drawnWinRate = 0.5,
        everDrawnGameCount = 1,
        everDrawnWinRate = everDrawnWinRate,
        neverDrawnGameCount = 1,
        neverDrawnWinRate = 0.5,
        drawnImprovementWinRate = 0.0,
    )
}
