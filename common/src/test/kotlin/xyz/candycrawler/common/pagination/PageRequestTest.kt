package xyz.candycrawler.common.pagination

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class PageRequestTest {

    @Test
    fun `valid page request is created successfully`() {
        val req = PageRequest(page = 0, size = 20, sortBy = "createdAt", sortDir = SortDir.DESC)
        assertEquals(0, req.page)
        assertEquals(20, req.size)
    }

    @Test
    fun `negative page throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(page = -1, size = 20, sortBy = "createdAt", sortDir = SortDir.ASC)
        }
    }

    @Test
    fun `size zero throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(page = 0, size = 0, sortBy = "createdAt", sortDir = SortDir.ASC)
        }
    }

    @Test
    fun `size over 100 throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(page = 0, size = 101, sortBy = "createdAt", sortDir = SortDir.ASC)
        }
    }

    @Test
    fun `size of exactly 100 is valid`() {
        val req = PageRequest(page = 0, size = 100, sortBy = "createdAt", sortDir = SortDir.ASC)
        assertEquals(100, req.size)
    }
}
