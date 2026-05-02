package xyz.candycrawler.draftsimparser.application.service

import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.port.LlmClient
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArticleAnalysisServiceTest {

    private val articleRepository = mock<ArticleRepository>()
    private val queryArticleRepository = mock<QueryArticleRepository>()
    private val vectorIndexService = mock<ArticleVectorIndexService>()
    private val llmClient = RecordingLlmClient()
    private val objectMapper = ObjectMapper()
    private val service = ArticleAnalysisService(
        llmClient = llmClient,
        articleRepository = articleRepository,
        queryArticleRepository = queryArticleRepository,
        promptBuilder = ArticleAnalysisPromptBuilder(),
        objectMapper = objectMapper,
        vectorIndexService = vectorIndexService,
    )

    @Test
    fun `consume classifies article before running specialized paragraph analysis`() = runBlocking {
        var storedArticle = article(
            textContent = """
                Station rewards tapping creatures.

                Build around cards with activated abilities.
            """.trimIndent(),
            keywords = listOf("station", "activated abilities"),
        )
        whenever(queryArticleRepository.findById(1)).thenReturn(storedArticle)
        whenever(articleRepository.update(eq(1), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(Article) -> Article>(1)
            block(storedArticle).also { storedArticle = it }
        }
        llmClient.classificationResponse = """
            {
              "article_type": "mechanic_guide",
              "processing_profile": "mechanic",
              "reason": "Title and keywords describe a mechanic",
              "confidence": 0.91
            }
        """.trimIndent()
        llmClient.analysisResponse = """
            {
              "type": "mechanic",
              "subject": "Station",
              "summary": "Station rewards tapping creatures for value.",
              "deckbuilding_implications": ["Prioritize creatures with activated abilities."],
              "related_cards": [],
              "mechanics": ["Station"],
              "archetypes": [],
              "formats": ["Limited"],
              "tags": ["mechanic", "enabler"],
              "confidence": 0.8
            }
        """.trimIndent()

        service.consume(ArticleAnalysisMessage(1))

        assertTrue(llmClient.prompts.first().contains("ARTICLE_CLASSIFICATION"))
        assertTrue(llmClient.prompts.drop(1).all { it.contains("ARTICLE_ANALYSIS") })
        assertTrue(llmClient.prompts.drop(1).all { it.contains("Processing profile: mechanic") })

        val analyzedText = assertNotNull(storedArticle.analyzedText)
        val json = objectMapper.readTree(analyzedText)
        assertEquals(2, json["schema_version"].asInt())
        assertEquals("mechanic_guide", json["article_type"].asString())
        assertEquals("mechanic", json["processing_profile"].asString())
        assertEquals(2, json["insights"].size())
    }

    @Test
    fun `consume skips paragraph analysis when classification says ignore`() = runBlocking {
        var storedArticle = article(textContent = "Subscribe to our newsletter.", keywords = emptyList())
        whenever(queryArticleRepository.findById(1)).thenReturn(storedArticle)
        whenever(articleRepository.update(eq(1), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(Article) -> Article>(1)
            block(storedArticle).also { storedArticle = it }
        }
        llmClient.classificationResponse = """
            {
              "article_type": "ignore",
              "processing_profile": "ignore",
              "reason": "No MTG knowledge",
              "confidence": 0.95
            }
        """.trimIndent()

        service.consume(ArticleAnalysisMessage(1))

        assertEquals(1, llmClient.prompts.size)
        val analyzedText = assertNotNull(storedArticle.analyzedText)
        val json = objectMapper.readTree(analyzedText)
        assertEquals("ignore", json["article_type"].asString())
        assertEquals(0, json["insights"].size())
    }

    @Test
    fun `consume stores flat object insights when paragraph response is an array`() = runBlocking {
        var storedArticle = article(textContent = "Station and exhaust reward activated abilities.", keywords = listOf("station"))
        whenever(queryArticleRepository.findById(1)).thenReturn(storedArticle)
        whenever(articleRepository.update(eq(1), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(Article) -> Article>(1)
            block(storedArticle).also { storedArticle = it }
        }
        llmClient.classificationResponse = """
            {
              "article_type": "mechanic_guide",
              "processing_profile": "mechanic",
              "reason": "Title and keywords describe mechanics",
              "confidence": 0.9
            }
        """.trimIndent()
        llmClient.analysisResponse = """
            [
              {
                "type": "mechanic",
                "subject": "Station",
                "summary": "Station rewards tapping creatures.",
                "deckbuilding_implications": [],
                "related_cards": [],
                "mechanics": ["Station"],
                "archetypes": [],
                "formats": ["Limited"],
                "tags": ["mechanic"],
                "confidence": 0.8
              },
              [
                {
                  "type": "mechanic",
                  "subject": "Exhaust",
                  "summary": "Exhaust is a one-shot activation.",
                  "deckbuilding_implications": [],
                  "related_cards": [],
                  "mechanics": ["Exhaust"],
                  "archetypes": [],
                  "formats": ["Limited"],
                  "tags": ["mechanic"],
                  "confidence": 0.8
                }
              ],
              "skip me"
            ]
        """.trimIndent()

        service.consume(ArticleAnalysisMessage(1))

        val analyzedText = assertNotNull(storedArticle.analyzedText)
        val json = objectMapper.readTree(analyzedText)
        assertEquals(2, json["insights"].size())
        assertTrue(json["insights"].all { it.isObject })
        assertEquals("Station", json["insights"][0]["subject"].asString())
        assertEquals("Exhaust", json["insights"][1]["subject"].asString())
    }

    private fun article(textContent: String?, keywords: List<String>) = Article(
        id = 1,
        externalId = 100,
        title = "New Set Mechanics Guide",
        slug = "new-set-mechanics-guide",
        url = "https://draftsim.com/new-set-mechanics-guide",
        htmlContent = null,
        textContent = textContent,
        analyzedText = null,
        keywords = keywords,
        favorite = false,
        errorMsg = null,
        analyzStartedAt = null,
        analyzEndedAt = null,
        publishedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        fetchedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
    )

    private class RecordingLlmClient : LlmClient {
        val prompts = mutableListOf<String>()
        var classificationResponse: String? = null
        var analysisResponse: String? = null

        override suspend fun complete(prompt: String): String? {
            prompts.add(prompt)
            return if ("ARTICLE_CLASSIFICATION" in prompt) classificationResponse else analysisResponse
        }
    }
}
