package xyz.candycrawler.draftsimparser.application.service

import org.springframework.beans.factory.support.DefaultListableBeanFactory
import tools.jackson.databind.ObjectMapper
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorStore
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleVectorIndexServiceTest {

    private val service = ArticleVectorIndexService(
        vectorStoreProvider = DefaultListableBeanFactory().getBeanProvider(ArticleVectorStore::class.java),
        objectMapper = ObjectMapper(),
        enabled = false,
    )

    @Test
    fun `buildDocuments converts schema v2 insights into vector documents`() {
        val documents = service.buildDocuments(
            article(
                analyzedText = """
                    {
                      "schema_version": 2,
                      "article_type": "mechanic_guide",
                      "processing_profile": "mechanic",
                      "insights": [
                        {
                          "type": "mechanic",
                          "subject": "Station",
                          "summary": "Station rewards tapping creatures.",
                          "deckbuilding_implications": ["Prioritize creatures with activated abilities."],
                          "related_cards": ["Helpful Automaton"],
                          "mechanics": ["Station"],
                          "archetypes": ["WU Tap"],
                          "formats": ["Limited"],
                          "tags": ["mechanic", "enabler"]
                        }
                      ]
                    }
                """.trimIndent()
            )
        )

        assertEquals(1, documents.size)
        val document = documents.single()
        assertEquals("draftsim-article-1-insight-0", document.id)
        assertTrue("Article: New Set Mechanics Guide" in document.content)
        assertTrue("Deckbuilding implications: Prioritize creatures with activated abilities." in document.content)
        assertEquals(1L, document.metadata["article_id"])
        assertEquals("mechanic_guide", document.metadata["article_type"])
        assertEquals(listOf("Station"), document.metadata["mechanics"])
    }

    private fun article(analyzedText: String?) = Article(
        id = 1,
        externalId = 100,
        title = "New Set Mechanics Guide",
        slug = "new-set-mechanics-guide",
        url = "https://draftsim.com/new-set-mechanics-guide",
        htmlContent = null,
        textContent = null,
        analyzedText = analyzedText,
        keywords = listOf("station"),
        favorite = true,
        errorMsg = null,
        analyzStartedAt = null,
        analyzEndedAt = null,
        publishedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        fetchedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
    )
}
