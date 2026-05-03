package xyz.candycrawler.collectionmanager.domain.card.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CardSearchCriteriaTest {

    @Test
    fun `default values`() {
        val criteria = CardSearchCriteria()

        assertEquals(null, criteria.query)
        assertEquals(null, criteria.setCode)
        assertEquals(null, criteria.rarity)
        assertEquals(CardSortOrder.NAME, criteria.order)
        assertEquals(SortDirection.AUTO, criteria.direction)
        assertEquals(1, criteria.page)
        assertEquals(CardSearchCriteria.DEFAULT_PAGE_SIZE, criteria.pageSize)
    }

    @Test
    fun `constants`() {
        assertEquals(20, CardSearchCriteria.DEFAULT_PAGE_SIZE)
        assertEquals(175, CardSearchCriteria.MAX_PAGE_SIZE)
    }

    @Test
    fun `all params can be set`() {
        val criteria = CardSearchCriteria(
            query = "bolt",
            setCode = "neo",
            rarity = "rare",
            order = CardSortOrder.CMC,
            direction = SortDirection.DESC,
            page = 3,
            pageSize = 50,
        )

        assertEquals("bolt", criteria.query)
        assertEquals("neo", criteria.setCode)
        assertEquals("rare", criteria.rarity)
        assertEquals(CardSortOrder.CMC, criteria.order)
        assertEquals(SortDirection.DESC, criteria.direction)
        assertEquals(3, criteria.page)
        assertEquals(50, criteria.pageSize)
    }
}
