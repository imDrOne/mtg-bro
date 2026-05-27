package xyz.candycrawler.draftsimparser.infrastructure.client.scryfall

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import xyz.candycrawler.draftsimparser.configuration.ActiveSetProperties
import xyz.candycrawler.scryfall.client.ScryfallApiClient
import xyz.candycrawler.scryfall.dto.response.ScryfallSetResponse
import xyz.candycrawler.scryfall.dto.response.ScryfallSetsResponse
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScryfallActiveSetAdapterTest {

    private val scryfallApiClient = mock<ScryfallApiClient>()
    private val defaultProperties = ActiveSetProperties(
        windowDays = 365,
        setTypes = listOf("expansion", "core", "draft_innovation", "commander", "masters"),
        includeDigital = false,
    )
    private val adapter = ScryfallActiveSetAdapter(scryfallApiClient, defaultProperties)

    @Test
    fun `sets outside the date window are filtered out`() {
        val today = LocalDate.now()
        val tooOld = today.minusDays(366).toString()
        val futureDate = today.plusDays(1).toString()
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "OLD", releasedAt = tooOld),
                set(code = "FUTURE", releasedAt = futureDate),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertTrue(result.isEmpty(), "Expected no sets but got: $result")
    }

    @Test
    fun `sets with excluded set type are filtered out`() {
        val today = LocalDate.now()
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "TOK", releasedAt = today.toString(), setType = "token"),
                set(code = "PROMO", releasedAt = today.toString(), setType = "promo"),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertTrue(result.isEmpty(), "Expected no sets but got: $result")
    }

    @Test
    fun `digital sets are filtered out when includeDigital is false`() {
        val today = LocalDate.now()
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "DIGI", releasedAt = today.toString(), digital = true),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertTrue(result.isEmpty(), "Expected no sets but got: $result")
    }

    @Test
    fun `digital sets are included when includeDigital is true`() {
        val today = LocalDate.now()
        val properties = defaultProperties.copy(includeDigital = true)
        val adapterWithDigital = ScryfallActiveSetAdapter(scryfallApiClient, properties)
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "DIGI", releasedAt = today.toString(), digital = true),
            ),
        )

        val result = adapterWithDigital.fetchActiveSets()

        assertEquals(1, result.size)
        assertEquals("DIGI", result[0].code)
    }

    @Test
    fun `sets with cardCount zero are filtered out`() {
        val today = LocalDate.now()
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "EMPTY", releasedAt = today.toString(), cardCount = 0),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertTrue(result.isEmpty(), "Expected no sets but got: $result")
    }

    @Test
    fun `result is sorted by releasedAt descending`() {
        val today = LocalDate.now()
        val older = today.minusDays(100).toString()
        val newer = today.minusDays(10).toString()
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "OLDER", releasedAt = older),
                set(code = "NEWER", releasedAt = newer),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertEquals(listOf("NEWER", "OLDER"), result.map { it.code })
    }

    @Test
    fun `set that passes all filters is included in result`() {
        val today = LocalDate.now()
        val releasedAt = today.minusDays(30).toString()
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(
                    code = "MKM",
                    name = "Murders at Karlov Manor",
                    releasedAt = releasedAt,
                    setType = "expansion",
                    cardCount = 286,
                    digital = false,
                ),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertEquals(1, result.size)
        assertEquals("MKM", result[0].code)
        assertEquals("Murders at Karlov Manor", result[0].name)
        assertEquals(LocalDate.parse(releasedAt), result[0].releasedAt)
        assertEquals("expansion", result[0].setType)
    }

    @Test
    fun `sets with null releasedAt are filtered out`() {
        whenever(scryfallApiClient.getSets()).thenReturn(
            setsResponse(
                set(code = "NODATE", releasedAt = null),
            ),
        )

        val result = adapter.fetchActiveSets()

        assertTrue(result.isEmpty(), "Expected no sets but got: $result")
    }

    private fun setsResponse(vararg sets: ScryfallSetResponse) = ScryfallSetsResponse(
        objectType = "list",
        hasMore = false,
        data = sets.toList(),
    )

    private fun set(
        code: String,
        name: String = code,
        releasedAt: String? = LocalDate.now().toString(),
        setType: String = "expansion",
        cardCount: Int = 100,
        digital: Boolean = false,
    ) = ScryfallSetResponse(
        code = code,
        name = name,
        setType = setType,
        releasedAt = releasedAt,
        cardCount = cardCount,
        digital = digital,
    )
}
