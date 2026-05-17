package xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ArticleRecord
import xyz.candycrawler.draftsimparser.lib.AbstractIntegrationTest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Transactional
class ArticleSqlMapperPreserveAnalyzedTextTest(@Autowired private val sqlMapper: ArticleSqlMapper) :
    AbstractIntegrationTest() {

    @Test
    fun `upsert preserves analyzedText, favorite, errorMsg on conflict`() {
        // 1. Insert article via upsert
        val initial = sqlMapper.upsert(
            ArticleRecord(
                id = null,
                externalId = 42L,
                title = "Original Title",
                slug = "original-slug",
                url = "https://example.com/original",
                htmlContent = "<p>original</p>",
                textContent = "original text",
                analyzedText = null,
                keywords = emptyList(),
                favorite = false,
                errorMsg = null,
                analyzStartedAt = null,
                analyzEndedAt = null,
                publishedAt = null,
                fetchedAt = LocalDateTime.now(),
            ),
        )
        val id = initial.id!!

        // 2. Set user-mutated fields via updateMutableFields
        val now = LocalDateTime.now()
        sqlMapper.updateMutableFields(
            id,
            initial.copy(
                analyzedText = """{"schema_version":2}""",
                favorite = true,
                errorMsg = "prior-error",
                analyzStartedAt = now,
                analyzEndedAt = now,
            ),
        )

        // 3. Re-upsert same externalId with updated content fields
        sqlMapper.upsert(
            ArticleRecord(
                id = null,
                externalId = 42L,
                title = "Updated Title",
                slug = "updated-slug",
                url = "https://example.com/updated",
                htmlContent = "<p>updated</p>",
                textContent = "updated text",
                analyzedText = null,
                keywords = emptyList(),
                favorite = false,
                errorMsg = null,
                analyzStartedAt = null,
                analyzEndedAt = null,
                publishedAt = null,
                fetchedAt = LocalDateTime.now(),
            ),
        )

        // 4. Assert: user-mutated fields preserved, content fields updated
        val result = sqlMapper.selectById(id)
        assertNotNull(result)

        // Content fields SHOULD be updated
        assertEquals("Updated Title", result.title)
        assertEquals("updated-slug", result.slug)
        assertEquals("<p>updated</p>", result.htmlContent)
        assertEquals("updated text", result.textContent)

        // User-mutated fields MUST be preserved
        assertEquals("""{"schema_version":2}""", result.analyzedText)
        assertEquals(true, result.favorite)
        assertEquals("prior-error", result.errorMsg)
        assertNotNull(result.analyzStartedAt)
        assertNotNull(result.analyzEndedAt)
    }
}
