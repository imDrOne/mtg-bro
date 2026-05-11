package xyz.candycrawler.mcpserver.tools

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpResponseUtilTest {

    private fun mockClient(status: HttpStatusCode, body: String): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = body,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        }

    @Test
    fun `200 OK with response text returns full text`() = runBlocking {
        val expectedBody = """{"card": "Lightning Bolt"}"""
        val client = mockClient(HttpStatusCode.OK, expectedBody)

        val result = client.get("http://test/endpoint").readTextOrFail("test-endpoint")

        assertEquals(expectedBody, result)
    }

    @Test
    fun `200 OK with 250 KB body returns full body`() = runBlocking {
        val largeBody = "x".repeat(250 * 1024)
        val client = mockClient(HttpStatusCode.OK, largeBody)

        val result = client.get("http://test/endpoint").readTextOrFail("test-endpoint")

        assertEquals(250 * 1024, result.length)
        assertEquals(largeBody, result)
    }

    @Test
    fun `401 Unauthorized with empty body throws DownstreamHttpException with status and endpointLabel`() = runBlocking {
        val client = mockClient(HttpStatusCode.Unauthorized, "")

        val exception = assertFailsWith<DownstreamHttpException> {
            client.get("http://test/endpoint").readTextOrFail("my-service")
        }

        assertEquals(401, exception.status)
        assertTrue("my-service" in exception.message!!)
        assertTrue("<empty>" in exception.message!!)
    }

    @Test
    fun `500 Internal Server Error with 500-char body truncates bodyPreview to MAX_ERROR_BODY_PREVIEW`() = runBlocking {
        val longBody = "e".repeat(500)
        val client = mockClient(HttpStatusCode.InternalServerError, longBody)

        val exception = assertFailsWith<DownstreamHttpException> {
            client.get("http://test/endpoint").readTextOrFail("downstream-api")
        }

        assertEquals(500, exception.status)
        assertEquals(MAX_ERROR_BODY_PREVIEW, exception.bodyPreview.length)
        assertEquals("e".repeat(MAX_ERROR_BODY_PREVIEW), exception.bodyPreview)
    }
}
