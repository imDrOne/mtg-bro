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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetDraftsimArticlesByIdHandlerTest {

    private val analyzedText =
        """{"schema_version":2,"article_type":"draft_guide","processing_profile":"draft","insights":[{"type":"card","subject":"Foo","summary":"Bar"},{"type":"card","subject":"Baz","summary":"Qux"}]}"""

    private val articleArrayJson =
        """[{"id":1,"title":"Test Article","keywords":[],"analyzedText":"${analyzedText.replace("\"", "\\\"")}"}]"""

    private fun makeRequest() = CallToolRequest(
        params = CallToolRequestParams(
            name = "get_draftsim_articles",
            arguments = buildJsonObject {
                put("ids", buildJsonArray { add(JsonPrimitive(1L)) })
            },
        ),
    )

    private fun contextWith(client: HttpClient) = ToolContext(
        baseUrl = "http://collection-manager",
        draftsimParserBaseUrl = "http://draftsim-parser",
        wizardStatAggregatorBaseUrl = "http://wizard-stat-aggregator",
        httpClient = client,
    )

    private fun mockClient(status: HttpStatusCode, body: String): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { _ ->
                    respond(
                        content = body,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        }

    @Test
    fun `200 OK with valid article array returns formatted report`() = runBlocking {
        val client = mockClient(HttpStatusCode.OK, articleArrayJson)
        val context = contextWith(client)

        val result = handleGetDraftsimArticlesById(context, makeRequest())

        assertFalse(result.isError == true, "Expected isError=false but got error")
        val text = result.content.filterIsInstance<io.modelcontextprotocol.kotlin.sdk.types.TextContent>()
            .joinToString("") { it.text }
        assertTrue("=== [1] Test Article ===" in text, "Expected article header in text. Got:\n$text")
        assertTrue("pagination:" in text, "Expected pagination info in text. Got:\n$text")
    }

    @Test
    fun `401 Unauthorized with empty body returns isError=true with HTTP 401 and endpoint label`() = runBlocking {
        val client = mockClient(HttpStatusCode.Unauthorized, "")
        val context = contextWith(client)

        val result = handleGetDraftsimArticlesById(context, makeRequest())

        assertTrue(result.isError == true, "Expected isError=true")
        val text = result.content.filterIsInstance<io.modelcontextprotocol.kotlin.sdk.types.TextContent>()
            .joinToString("") { it.text }
        assertTrue("HTTP 401" in text, "Expected 'HTTP 401' in error text. Got:\n$text")
        assertTrue("draftsim-parser" in text, "Expected 'draftsim-parser' in error text. Got:\n$text")
    }

    @Test
    fun `500 Internal Server Error returns isError=true with HTTP 500`() = runBlocking {
        val client = mockClient(HttpStatusCode.InternalServerError, """{"error":"oops"}""")
        val context = contextWith(client)

        val result = handleGetDraftsimArticlesById(context, makeRequest())

        assertTrue(result.isError == true, "Expected isError=true")
        val text = result.content.filterIsInstance<io.modelcontextprotocol.kotlin.sdk.types.TextContent>()
            .joinToString("") { it.text }
        assertNotNull(text)
        assertTrue("HTTP 500" in text, "Expected 'HTTP 500' in error text. Got:\n$text")
    }
}
