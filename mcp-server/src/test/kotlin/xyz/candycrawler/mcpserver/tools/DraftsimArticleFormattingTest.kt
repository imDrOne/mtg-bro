package xyz.candycrawler.mcpserver.tools

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DraftsimArticleFormattingTest {

    @Test
    fun `get articles flattens filters and paginates normalized insights`() {
        val report = formatDraftsimArticlesReport(
            articles = articleArray(
                analyzedText = """
                    {
                      "schema_version": 2,
                      "article_type": "set_review",
                      "processing_profile": "limited",
                      "insights": [
                        {"type": "mechanic", "subject": "Station", "summary": "Tap creatures for value."},
                        [
                          {"type": "archetype", "subject": "WU Tap", "summary": "Draft payoffs."},
                          "skip me",
                          [
                            {"type": "MECHANIC", "subject": "Exhaust", "summary": "One-shot activated abilities."}
                          ]
                        ],
                        42
                      ]
                    }
                """.trimIndent()
            ),
            types = setOf("mechanic"),
            page = 2,
            pageSize = 1,
        )

        assertTrue("article_type: set_review" in report)
        assertTrue("processing_profile: limited" in report)
        assertTrue("pagination: total=2, page=2, page_size=1, has_more=false" in report)
        assertTrue("Exhaust (type=MECHANIC)" in report)
        assertFalse("Station (type=mechanic)" in report)
        assertFalse("WU Tap" in report)
        assertFalse("skip me" in report)
    }

    @Test
    fun `get articles does not return raw analyzedText payload`() {
        val report = formatDraftsimArticlesReport(
            articles = articleArray(
                analyzedText = """
                    {
                      "schema_version": 2,
                      "article_type": "mechanic_guide",
                      "processing_profile": "mechanic",
                      "raw_body": "THIS RAW ANALYZED TEXT SHOULD NOT BE RETURNED",
                      "insights": [
                        {"type": "mechanic", "subject": "Station", "summary": "Compact summary."}
                      ]
                    }
                """.trimIndent()
            ),
        )

        assertTrue("Compact summary." in report)
        assertFalse("THIS RAW ANALYZED TEXT SHOULD NOT BE RETURNED" in report)
        assertFalse("\"raw_body\"" in report)
    }

    @Test
    fun `list articles formats compact metadata without raw analyzedText`() {
        val response = formatDraftsimArticleList(
            Json.parseToJsonElement(
                """
                    {
                      "articles": [
                        {
                          "id": 12,
                          "title": "Limited Guide",
                          "slug": "limited-guide",
                          "url": "https://draftsim.com/limited-guide",
                          "publishedAt": "2026-01-04T00:00:00",
                          "keywords": ["limited", "draft"],
                          "favorite": true,
                          "analyzedText": "RAW ANALYSIS SHOULD NOT BE PRINTED"
                        }
                      ],
                      "totalArticles": 42,
                      "page": 2,
                      "pageSize": 20,
                      "hasMore": true
                    }
                """.trimIndent()
            ).jsonObject
        )

        assertTrue(response != null)
        assertTrue("Found 1 Draftsim articles (total: 42, page: 2, page_size: 20, has_more: true)" in response)
        assertTrue("[id=12] Limited Guide - limited-guide (2026-01-04)" in response)
        assertTrue("url: https://draftsim.com/limited-guide" in response)
        assertTrue("keywords: limited, draft" in response)
        assertTrue("favorite: true, analyzed: true" in response)
        assertFalse("RAW ANALYSIS SHOULD NOT BE PRINTED" in response)
    }

    @Test
    fun `list article options default favorite true and clamp page size`() {
        val options = parseDraftsimArticleListOptions(
            buildJsonObject {
                put("q", " station ")
                put("page", 0)
                put("page_size", 500)
            }
        )

        assertEquals("station", options.query)
        assertEquals(1, options.page)
        assertEquals(100, options.pageSize)
        assertEquals(true, options.favorite)
    }

    @Test
    fun `semantic search response includes filtered preview matches with limit`() {
        val response = formatDraftsimSemanticSearchResponse(
            json = Json.parseToJsonElement(
                """
                    {
                      "results": [
                        {
                          "article": {
                            "id": 7,
                            "title": "Set Guide",
                            "slug": "set-guide",
                            "publishedAt": "2026-01-02T00:00:00"
                          },
                          "score": 0.87654,
                          "matches": [
                            {
                              "subject": "Station",
                              "insightType": "mechanic",
                              "content": "Station rewards tapping creatures.\nIt is best with cheap bodies.",
                              "tags": ["mechanic", "tap"]
                            },
                            {
                              "subject": "WU Tap",
                              "insightType": "archetype",
                              "content": "Draft WU payoffs.",
                              "tags": ["archetype"]
                            },
                            {
                              "subject": "Exhaust",
                              "insightType": "mechanic",
                              "content": "Exhaust is a one-shot activation.",
                              "tags": ["mechanic"]
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            ).jsonObject,
            types = setOf("mechanic"),
            previewLimit = 1,
            similarityThreshold = DRAFTSIM_SIMILARITY_MEDIUM,
            semanticAttempts = 2,
        )

        assertTrue(response != null)
        assertTrue("[id=7] Set Guide - set-guide (2026-01-02) score=0.877" in response)
        assertTrue("similarity_threshold: 0.65" in response)
        assertTrue("semantic_attempts: 2" in response)
        assertTrue("preview 1: subject=Station; type=mechanic" in response)
        assertTrue("excerpt=Station rewards tapping creatures. It is best with cheap bodies." in response)
        assertTrue("tags=mechanic, tap" in response)
        assertFalse("WU Tap" in response)
        assertFalse("Exhaust" in response)
        assertFalse("preview 2:" in response)
    }

    @Test
    fun `semantic search response returns null when type filter removes all previews`() {
        val response = formatDraftsimSemanticSearchResponse(
            json = Json.parseToJsonElement(
                """
                    {
                      "results": [
                        {
                          "article": {
                            "id": 7,
                            "title": "Set Guide",
                            "slug": "set-guide",
                            "publishedAt": "2026-01-02T00:00:00"
                          },
                          "score": 0.87654,
                          "matches": [
                            {
                              "subject": "WU Tap",
                              "insightType": "archetype",
                              "content": "Draft WU payoffs.",
                              "tags": ["archetype"]
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            ).jsonObject,
            types = setOf("mechanic"),
        )

        assertNull(response)
    }

    @Test
    fun `semantic search response drops only results without matching filtered previews`() {
        val response = formatDraftsimSemanticSearchResponse(
            json = Json.parseToJsonElement(
                """
                    {
                      "results": [
                        {
                          "article": {
                            "id": 7,
                            "title": "Archetype Guide",
                            "slug": "archetype-guide",
                            "publishedAt": "2026-01-02T00:00:00"
                          },
                          "score": 0.91,
                          "matches": [
                            {
                              "subject": "WU Tap",
                              "insightType": "archetype",
                              "content": "Draft WU payoffs.",
                              "tags": ["archetype"]
                            }
                          ]
                        },
                        {
                          "article": {
                            "id": 8,
                            "title": "Mechanic Guide",
                            "slug": "mechanic-guide",
                            "publishedAt": "2026-01-03T00:00:00"
                          },
                          "score": 0.88,
                          "matches": [
                            {
                              "subject": "Station",
                              "insightType": "mechanic",
                              "content": "Station rewards tapping creatures.",
                              "tags": ["mechanic"]
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            ).jsonObject,
            types = setOf("mechanic"),
        )

        assertTrue(response != null)
        assertTrue("Found 1 semantically relevant articles" in response)
        assertTrue("Mechanic Guide" in response)
        assertFalse("Archetype Guide" in response)
    }

    @Test
    fun `empty semantic response allows fallback keyword article search`() {
        val semantic = formatDraftsimSemanticSearchResponse(
            Json.parseToJsonElement("""{"results": []}""").jsonObject
        )
        val fallback = formatDraftsimFallbackSearchResponse(
            json = Json.parseToJsonElement(
                """
                    {
                      "articles": [
                        {
                          "id": 3,
                          "title": "Draft Guide",
                          "slug": "draft-guide",
                          "publishedAt": "2026-01-01T00:00:00"
                        }
                      ],
                      "totalArticles": 1,
                      "hasMore": false
                    }
                """.trimIndent()
            ).jsonObject,
            query = "station",
            exhaustedSimilarityThresholds = DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS,
        )

        assertNull(semantic)
        assertTrue(fallback != null)
        assertTrue("fallback: keyword article search for 'station'" in fallback)
        assertTrue("semantic_thresholds_exhausted: 0.80, 0.65, 0.50" in fallback)
        assertTrue("[id=3] Draft Guide - draft-guide (2026-01-01)" in fallback)
    }

    @Test
    fun `semantic summary tries thresholds from high to low until one formats`() = runBlocking {
        val searchedThresholds = mutableListOf<Double>()
        val formattedThresholds = mutableListOf<Double>()

        val summary = findDraftsimSemanticSummary(
            thresholds = DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS,
            search = { threshold ->
                searchedThresholds += threshold
                Json.parseToJsonElement("""{"results": []}""").jsonObject
            },
            format = { _, threshold, attempt ->
                formattedThresholds += threshold
                if (threshold == DRAFTSIM_SIMILARITY_MEDIUM) "semantic result at $threshold after $attempt attempts" else null
            },
        )

        assertEquals("semantic result at 0.65 after 2 attempts", summary)
        assertEquals(listOf(DRAFTSIM_SIMILARITY_HIGH, DRAFTSIM_SIMILARITY_MEDIUM), searchedThresholds)
        assertEquals(listOf(DRAFTSIM_SIMILARITY_HIGH, DRAFTSIM_SIMILARITY_MEDIUM), formattedThresholds)
    }

    @Test
    fun `semantic summary skips failed threshold search attempts`() = runBlocking {
        val searchedThresholds = mutableListOf<Double>()

        val summary = findDraftsimSemanticSummary(
            thresholds = DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS,
            search = { threshold ->
                searchedThresholds += threshold
                if (threshold == DRAFTSIM_SIMILARITY_HIGH) error("semantic search failed")
                Json.parseToJsonElement("""{"results": []}""").jsonObject
            },
            format = { _, threshold, attempt ->
                if (threshold == DRAFTSIM_SIMILARITY_MEDIUM) "semantic result after $attempt attempts" else null
            },
        )

        assertEquals("semantic result after 2 attempts", summary)
        assertEquals(listOf(DRAFTSIM_SIMILARITY_HIGH, DRAFTSIM_SIMILARITY_MEDIUM), searchedThresholds)
    }

    private fun articleArray(analyzedText: String) = buildJsonArray {
        add(
            buildJsonObject {
                put("id", 1)
                put("title", "New Set Mechanics Guide")
                put("keywords", buildJsonArray {
                    add("station")
                    add("limited")
                })
                put("analyzedText", analyzedText)
            }
        )
    }
}
