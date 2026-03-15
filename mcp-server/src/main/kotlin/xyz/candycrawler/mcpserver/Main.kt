package xyz.candycrawler.mcpserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val MCP_ACCEPT = "application/json, text/event-stream"
private const val SUPPORTED_PROTOCOL_VERSION = "2025-03-26"
private val jsonCodec = Json { ignoreUnknownKeys = true }

private fun rewriteProtocolVersion(body: String): String {
    return try {
        val json = jsonCodec.parseToJsonElement(body).jsonObject
        val params = json["params"]?.jsonObject ?: return body
        val clientVersion = params["protocolVersion"]?.jsonPrimitive?.content ?: return body
        if (clientVersion == SUPPORTED_PROTOCOL_VERSION) return body

        System.err.println("[MCP]   rewriting protocolVersion $clientVersion → $SUPPORTED_PROTOCOL_VERSION")
        val newParams = buildJsonObject {
            params.forEach { (k, v) ->
                if (k == "protocolVersion") put(k, JsonPrimitive(SUPPORTED_PROTOCOL_VERSION))
                else put(k, v)
            }
        }
        val newJson = buildJsonObject {
            json.forEach { (k, v) ->
                if (k == "params") put(k, newParams)
                else put(k, v)
            }
        }
        jsonCodec.encodeToString(JsonObject.serializer(), newJson)
    } catch (e: Exception) {
        System.err.println("[MCP]   protocolVersion rewrite failed: ${e.message}")
        body
    }
}

private fun ensureProtocolVersion(body: ByteArray): ByteArray {
    return try {
        val text = String(body)
        val json = jsonCodec.parseToJsonElement(text).jsonObject
        val result = json["result"]?.jsonObject ?: return body
        if (result.containsKey("protocolVersion")) return body
        if (!result.containsKey("serverInfo")) return body

        System.err.println("[MCP]   injecting protocolVersion into response")
        val newResult = buildJsonObject {
            put("protocolVersion", JsonPrimitive(SUPPORTED_PROTOCOL_VERSION))
            result.forEach { (k, v) -> put(k, v) }
        }
        val newJson = buildJsonObject {
            json.forEach { (k, v) ->
                if (k == "result") put(k, newResult)
                else put(k, v)
            }
        }
        jsonCodec.encodeToString(JsonObject.serializer(), newJson).toByteArray()
    } catch (e: Exception) {
        System.err.println("[MCP]   protocolVersion inject failed: ${e.message}")
        body
    }
}

fun main(args: Array<String>) {
    val baseUrl = System.getenv("COLLECTION_MANAGER_BASE_URL") ?: "http://localhost:8080"
    val transportArg = when {
        args.contains("--transport") -> {
            val idx = args.indexOf("--transport") + 1
            if (idx < args.size) args[idx] else "stdio"
        }
        else -> System.getenv("MCP_TRANSPORT") ?: "stdio"
    }
    val port = when {
        args.contains("--port") -> {
            val idx = args.indexOf("--port") + 1
            if (idx < args.size) args[idx].toIntOrNull() ?: 3000 else 3000
        }
        else -> System.getenv("MCP_HTTP_PORT")?.toIntOrNull() ?: 3000
    }

    val server = createServer(baseUrl)

    when (transportArg) {
        "stdio" -> {
            runBlocking {
                val stdioTransport = StdioServerTransport(
                    inputStream = System.`in`.asSource().buffered(),
                    outputStream = System.out.asSink().buffered()
                )
                server.createSession(stdioTransport)
            }
        }
        "http" -> {
            val internalPort = port + 1

            embeddedServer(ServerCIO, port = internalPort) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                mcpStreamableHttp { server }
            }.start(wait = false)

            System.err.println("[MCP] Internal handler on port $internalPort")
            System.err.println("[MCP] Proxy on port $port")

            val proxyClient = HttpClient(ClientCIO)

            embeddedServer(ServerCIO, port = port) {
                routing {
                    route("{...}") {
                        handle {
                            val method = call.request.httpMethod
                            val uri = call.request.uri
                            val incomingAccept = call.request.header("Accept") ?: ""
                            val sessionId = call.request.header("mcp-session-id") ?: "-"
                            System.err.println("[MCP] ${method.value} $uri  Accept=$incomingAccept  Session=$sessionId")

                            if (method == HttpMethod.Get && call.request.path() == "/mcp") {
                                System.err.println("[MCP]   → 405 Method Not Allowed")
                                call.respond(HttpStatusCode.MethodNotAllowed)
                                return@handle
                            }

                            var requestBody = if (method == HttpMethod.Post || method == HttpMethod.Delete) {
                                call.receiveText()
                            } else null

                            if (requestBody != null && method == HttpMethod.Post) {
                                requestBody = rewriteProtocolVersion(requestBody)
                            }
                            System.err.println("[MCP]   req body: ${requestBody?.take(300) ?: ""}")

                            val upstream = proxyClient.request("http://localhost:$internalPort$uri") {
                                this.method = method
                                headers {
                                    val incoming = call.request.headers
                                    incoming.names().forEach { name ->
                                        when {
                                            name.equals("Accept", ignoreCase = true) ->
                                                append("Accept", MCP_ACCEPT)
                                            name.equals("Host", ignoreCase = true) -> {}
                                            name.equals("Content-Length", ignoreCase = true) -> {}
                                            else -> incoming.getAll(name)?.forEach { append(name, it) }
                                        }
                                    }
                                }
                                if (requestBody != null) {
                                    setBody(requestBody)
                                }
                            }

                            val skipHeaders = setOf(
                                "Content-Length", "Transfer-Encoding", "Connection", "Keep-Alive"
                            )
                            upstream.headers.names().forEach { name ->
                                if (name !in skipHeaders) {
                                    upstream.headers.getAll(name)?.forEach { value ->
                                        call.response.headers.append(name, value, safeOnly = false)
                                    }
                                }
                            }
                            var body = upstream.readRawBytes()
                            val status = upstream.status

                            if (method == HttpMethod.Post && status == HttpStatusCode.OK) {
                                body = ensureProtocolVersion(body)
                            }

                            val respSessionId = upstream.headers["mcp-session-id"] ?: "-"
                            System.err.println("[MCP]   → ${status.value} ${status.description}  Session=$respSessionId  Body=${String(body).take(300)}")
                            call.respondBytes(body, upstream.contentType(), status)
                        }
                    }
                }
            }.start(wait = true)
        }
        else -> {
            System.err.println("Unknown transport: $transportArg. Use stdio or http.")
            System.exit(1)
        }
    }
}
