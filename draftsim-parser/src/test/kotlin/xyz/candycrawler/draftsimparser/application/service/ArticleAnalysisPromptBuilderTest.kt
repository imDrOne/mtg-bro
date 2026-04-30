package xyz.candycrawler.draftsimparser.application.service

import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleAnalysisPromptBuilderTest {

    private val builder = ArticleAnalysisPromptBuilder()

    @Test
    fun `classification prompt includes article metadata and keywords`() {
        val prompt = builder.buildClassificationPrompt(article())

        assertTrue("ARTICLE_CLASSIFICATION" in prompt)
        assertTrue("Article title: New Set Mechanics Guide" in prompt)
        assertTrue("Article slug: new-set-mechanics-guide" in prompt)
        assertTrue("Extracted keywords: station, draft strategy" in prompt)
    }

    @Test
    fun `mechanic classification uses mechanic analysis instructions`() {
        val prompt = builder.buildAnalysisPrompt(
            paragraph = "Station rewards creatures that can tap for value.",
            article = article(),
            classification = ArticleAnalysisClassification(
                articleType = ArticleType.MECHANIC_GUIDE,
                processingProfile = ProcessingProfile.MECHANIC,
                reason = "Title mentions mechanics",
                confidence = 0.9,
            ),
        )

        assertTrue("ARTICLE_ANALYSIS" in prompt)
        assertTrue("Processing profile: mechanic" in prompt)
        assertTrue("how the mechanic works" in prompt)
        assertTrue("Station rewards creatures" in prompt)
    }

    @Test
    fun `unknown classification falls back to generic and article type defaults profile`() {
        assertEquals(ArticleType.GENERIC_MTG_KNOWLEDGE, ArticleType.from("unknown"))
        assertEquals(ProcessingProfile.MECHANIC, ProcessingProfile.from(null, ArticleType.MECHANIC_GUIDE))
    }

    private fun article() = Article(
        id = 1,
        externalId = 100,
        title = "New Set Mechanics Guide",
        slug = "new-set-mechanics-guide",
        url = "https://draftsim.com/new-set-mechanics-guide",
        htmlContent = null,
        textContent = "Station rewards creatures that can tap for value.",
        analyzedText = null,
        keywords = listOf("station", "draft strategy"),
        favorite = false,
        errorMsg = null,
        analyzStartedAt = null,
        analyzEndedAt = null,
        publishedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        fetchedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
    )
}
