package xyz.candycrawler.mcpserver.tools

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchDraftsimArticlesHandlerTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun makeRequest() = CallToolRequest(
        params = CallToolRequestParams(
            name = "search_draftsim_articles",
            arguments = buildJsonObject { put("query", "draft guide") },
        ),
    )

    private fun contextWith(client: HttpClient) = ToolContext(
        baseUrl = "http://collection-manager",
        draftsimParserBaseUrl = "http://draftsim-parser",
        wizardStatAggregatorBaseUrl = "http://wizard-stat-aggregator",
        httpClient = client,
    )

    private fun resultText(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        result.content.filterIsInstance<TextContent>().joinToString("") { it.text }

    @Test
    fun `semantic 401 falls back to keyword search and returns fallback result`() = runBlocking {
        val fallbackJson = """
            {
              "articles": [
                {
                  "id": 1,
                  "title": "Draft Guide",
                  "slug": "draft-guide",
                  "url": "https://example.com",
                  "publishedAt": "2026-01-01T00:00:00",
                  "keywords": [],
                  "favorite": false
                }
              ],
              "totalArticles": 1,
              "page": 1,
              "pageSize": 10,
              "hasMore": false
            }
        """.trimIndent()

        var callCount = 0
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    when (callCount++) {
                        0 -> respond(content = "", status = HttpStatusCode.Unauthorized, headers = jsonHeaders)
                        else -> respond(content = fallbackJson, status = HttpStatusCode.OK, headers = jsonHeaders)
                    }
                }
            }
        }

        val result = handleSearchDraftsimArticles(contextWith(client), makeRequest())

        assertFalse(result.isError == true, "Expected isError=false but got error")
        val text = resultText(result)
        assertTrue("Draft Guide" in text, "Expected 'Draft Guide' in result. Got:\n$text")
    }

    @Test
    fun `semantic 200 empty results then fallback 500 returns isError=true with HTTP 500`() = runBlocking {
        val semanticEmptyJson = """{"results":[]}"""
        val fallback500Json = """{"error":"db failure"}"""

        var callCount = 0
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    when (callCount++) {
                        0 -> respond(content = semanticEmptyJson, status = HttpStatusCode.OK, headers = jsonHeaders)
                        else -> respond(
                            content = fallback500Json,
                            status = HttpStatusCode.InternalServerError,
                            headers = jsonHeaders,
                        )
                    }
                }
            }
        }

        val result = handleSearchDraftsimArticles(contextWith(client), makeRequest())

        assertTrue(result.isError == true, "Expected isError=true")
        val text = resultText(result)
        assertTrue("HTTP 500" in text, "Expected 'HTTP 500' in error text. Got:\n$text")
    }

    @Test
    fun `semantic 200 with results returns semantic result without calling fallback`() = runBlocking {
        val semanticJson = """
            {
              "results": [
                {
                  "article": {
                    "id": 7,
                    "title": "Set Guide",
                    "slug": "sg",
                    "publishedAt": "2026-01-02T00:00:00"
                  },
                  "score": 0.87,
                  "matches": [
                    {
                      "subject": "Station",
                      "insightType": "mechanic",
                      "content": "foo",
                      "tags": []
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        var callCount = 0
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    callCount++
                    respond(content = semanticJson, status = HttpStatusCode.OK, headers = jsonHeaders)
                }
            }
        }

        val result = handleSearchDraftsimArticles(contextWith(client), makeRequest())

        assertFalse(result.isError == true, "Expected isError=false but got error")
        val text = resultText(result)
        assertTrue("Set Guide" in text, "Expected 'Set Guide' in result. Got:\n$text")
        assertTrue("Station" in text, "Expected 'Station' in result. Got:\n$text")
        // Semantic succeeded at first threshold — only semantic calls (1 per threshold attempt until success),
        // no fallback call expected.
        assertTrue(callCount == 1, "Expected only 1 HTTP call (semantic succeeded), but got $callCount")
    }
}
