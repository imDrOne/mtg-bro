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

class SearchLimitedCardStatsHandlerTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun makeRequest() = CallToolRequest(
        params = CallToolRequestParams(
            name = "search_limited_card_stats",
            arguments = buildJsonObject { put("set_code", "sos") },
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
    fun `200 OK with valid response returns formatted output`() = runBlocking {
        val responseJson = """
            {
              "data": [
                {
                  "name": "Stone Docent",
                  "mtgaId": 12345,
                  "tier": "A",
                  "rarity": "R",
                  "color": "U",
                  "winRate": 0.59,
                  "gameCount": 1000,
                  "avgPick": 2.5,
                  "drawnWinRate": 0.61,
                  "drawnImprovementWinRate": 0.04
                }
              ],
              "totalStats": 1,
              "hasMore": false,
              "page": 1,
              "pageSize": 20
            }
        """.trimIndent()

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    respond(content = responseJson, status = HttpStatusCode.OK, headers = jsonHeaders)
                }
            }
        }

        val result = handleSearchLimitedCardStats(contextWith(client), makeRequest())

        assertFalse(result.isError == true, "Expected isError=false but got error")
        val text = resultText(result)
        assertTrue("Stone Docent" in text, "Expected 'Stone Docent' in result. Got:\n$text")
        assertTrue("59.0%" in text, "Expected '59.0%' in result. Got:\n$text")
    }

    @Test
    fun `401 returns isError=true with session-expired message`() = runBlocking {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    respond(content = "", status = HttpStatusCode.Unauthorized, headers = jsonHeaders)
                }
            }
        }

        val result = handleSearchLimitedCardStats(contextWith(client), makeRequest())

        assertTrue(result.isError == true, "Expected isError=true")
        val text = resultText(result)
        assertTrue("session expired" in text, "Expected 'session expired' in error text. Got:\n$text")
    }

    @Test
    fun `500 with error body returns isError=true with HTTP 500`() = runBlocking {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    respond(
                        content = """{"error":"internal server error"}""",
                        status = HttpStatusCode.InternalServerError,
                        headers = jsonHeaders,
                    )
                }
            }
        }

        val result = handleSearchLimitedCardStats(contextWith(client), makeRequest())

        assertTrue(result.isError == true, "Expected isError=true")
        val text = resultText(result)
        assertTrue("HTTP 500" in text, "Expected 'HTTP 500' in error text. Got:\n$text")
    }
}
