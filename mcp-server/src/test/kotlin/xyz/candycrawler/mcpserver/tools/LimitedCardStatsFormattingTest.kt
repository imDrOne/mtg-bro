package xyz.candycrawler.mcpserver.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

class LimitedCardStatsFormattingTest {

    @Test
    fun `formats compact limited stats report`() {
        val report = formatLimitedCardStatsResponse(
            json = Json.parseToJsonElement(
                """
                    {
                      "totalStats": 42,
                      "hasMore": true,
                      "page": 1,
                      "pageSize": 2,
                      "data": [
                        {
                          "name": "Premium Common",
                          "mtgaId": 123,
                          "tier": "A+",
                          "color": "G",
                          "rarity": "common",
                          "winRate": 0.6123,
                          "gameCount": 1500,
                          "avgPick": 3.45,
                          "drawnWinRate": 0.654,
                          "drawnImprovementWinRate": 0.048
                        }
                      ]
                    }
                """.trimIndent(),
            ).jsonObject,
            setCode = "dmu",
            matchType = "QuickDraft",
        )

        assertTrue("Limited stats for DMU (QuickDraft): returned=1, total=42" in report)
        assertTrue("Premium Common | mtga_id: 123 | tier: A+ | G common | WR: 61.2%" in report)
        assertTrue("games: 1500" in report)
        assertTrue("ALSA: 3.45" in report)
        assertTrue("drawn WR: 65.4%" in report)
        assertTrue("IWD: +4.8%" in report)
    }
}
