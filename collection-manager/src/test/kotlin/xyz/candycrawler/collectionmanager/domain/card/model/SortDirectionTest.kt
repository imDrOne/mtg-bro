package xyz.candycrawler.collectionmanager.domain.card.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SortDirectionTest {

    @Test
    fun `fromString returns AUTO for null`() {
        assertEquals(SortDirection.AUTO, SortDirection.fromString(null))
    }

    @Test
    fun `fromString returns matching enum for valid value`() {
        assertEquals(SortDirection.AUTO, SortDirection.fromString("auto"))
        assertEquals(SortDirection.AUTO, SortDirection.fromString("AUTO"))
        assertEquals(SortDirection.ASC, SortDirection.fromString("asc"))
        assertEquals(SortDirection.ASC, SortDirection.fromString("ASC"))
        assertEquals(SortDirection.DESC, SortDirection.fromString("desc"))
        assertEquals(SortDirection.DESC, SortDirection.fromString("DESC"))
    }

    @Test
    fun `fromString returns AUTO for unknown value`() {
        assertEquals(SortDirection.AUTO, SortDirection.fromString("unknown"))
        assertEquals(SortDirection.AUTO, SortDirection.fromString(""))
    }
}
