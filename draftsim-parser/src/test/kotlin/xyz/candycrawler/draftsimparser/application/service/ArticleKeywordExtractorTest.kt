package xyz.candycrawler.draftsimparser.application.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleKeywordExtractorTest {

    private val extractor = ArticleKeywordExtractor()

    @Test
    fun `returns empty list for blank text`() {
        assertEquals(emptyList(), extractor.extract(null))
        assertEquals(emptyList(), extractor.extract("   "))
    }

    @Test
    fun `removes stop words and short tokens`() {
        val result = extractor.extract("The and but to in of are was you your", limit = 20)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `prioritizes repeated meaningful phrases`() {
        val result = extractor.extract(
            """
            The best removal spell is a removal spell.
            Removal wins draft games.
            Draft strategy rewards another removal spell.
            """.trimIndent(),
            limit = 5,
        )

        assertEquals("removal spell", result.first())
        assertTrue("removal" in result)
        assertTrue("draft" in result)
    }

    @Test
    fun `respects result limit`() {
        val text = (1..30).joinToString(" ") { "keyword$it" }

        val result = extractor.extract(text, limit = 10)

        assertEquals(10, result.size)
    }
}
