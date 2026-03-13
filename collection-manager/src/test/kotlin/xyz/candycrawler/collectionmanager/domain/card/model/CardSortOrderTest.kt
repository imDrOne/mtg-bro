package xyz.candycrawler.collectionmanager.domain.card.model

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CardSortOrderTest {

    @Test
    fun `fromString returns NAME for null`() {
        assertEquals(CardSortOrder.NAME, CardSortOrder.fromString(null))
    }

    @Test
    fun `fromString returns matching enum for valid value`() {
        assertEquals(CardSortOrder.NAME, CardSortOrder.fromString("name"))
        assertEquals(CardSortOrder.NAME, CardSortOrder.fromString("NAME"))
        assertEquals(CardSortOrder.SET, CardSortOrder.fromString("set"))
        assertEquals(CardSortOrder.RELEASED, CardSortOrder.fromString("released"))
        assertEquals(CardSortOrder.RARITY, CardSortOrder.fromString("rarity"))
        assertEquals(CardSortOrder.CMC, CardSortOrder.fromString("cmc"))
        assertEquals(CardSortOrder.USD, CardSortOrder.fromString("usd"))
        assertEquals(CardSortOrder.ARTIST, CardSortOrder.fromString("artist"))
    }

    @Test
    fun `fromString returns NAME for unknown value`() {
        assertEquals(CardSortOrder.NAME, CardSortOrder.fromString("unknown"))
        assertEquals(CardSortOrder.NAME, CardSortOrder.fromString(""))
    }
}
