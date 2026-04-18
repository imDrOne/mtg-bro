package xyz.candycrawler.mcpserver

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import java.util.concurrent.ConcurrentHashMap

private const val MCP_SESSION_ID_HEADER = "mcp-session-id"

fun Application.mcpFilteredStreamableHttp(path: String = "/mcp", server: FilteredMcpServer) {
    install(SSE)

    val transports = ConcurrentHashMap<String, StreamableHttpServerTransport>()

    fun getTransport(sessionId: String?): StreamableHttpServerTransport? =
        if (sessionId != null) transports[sessionId] else null

    routing {
        route(path) {
            sse {
                val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
                val transport = getTransport(sessionId)
                if (transport == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@sse
                }
                transport.handleRequest(this, call)
            }

            post {
                val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
                if (sessionId != null) {
                    val transport = getTransport(sessionId)
                    if (transport == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@post
                    }
                    transport.handleRequest(null, call)
                } else {
                    val transport = StreamableHttpServerTransport(
                        StreamableHttpServerTransport.Configuration(enableJsonResponse = true)
                    )
                    transport.setOnSessionInitialized { id -> transports[id] = transport }
                    transport.setOnSessionClosed { id -> transports.remove(id) }
                    server.onClose { transport.sessionId?.let { transports.remove(it) } }
                    server.createFilteredSession(transport)
                    transport.handleRequest(null, call)
                }
            }

            delete {
                val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
                val transport = getTransport(sessionId)
                if (transport == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                transport.handleRequest(null, call)
            }
        }
    }
}
