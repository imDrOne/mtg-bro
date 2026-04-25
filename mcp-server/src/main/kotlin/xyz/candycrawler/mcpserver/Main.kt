package xyz.candycrawler.mcpserver

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import xyz.candycrawler.mcpserver.auth.McpAuthPlugin
import xyz.candycrawler.mcpserver.auth.UserRolesElement
import xyz.candycrawler.mcpserver.auth.UserRolesKey
import xyz.candycrawler.mcpserver.auth.UserTokenElement
import xyz.candycrawler.mcpserver.auth.UserTokenKey
import xyz.candycrawler.mcpserver.auth.oauthMetadataRoutes
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

private const val MCP_SESSION_ID_HEADER = "mcp-session-id"

fun main(args: Array<String>) {
    val baseUrl = System.getenv("COLLECTION_MANAGER_BASE_URL") ?: "http://localhost:8080"
    val draftsimParserBaseUrl = System.getenv("DRAFTSIM_PARSER_BASE_URL") ?: "http://localhost:8081"
    val transport = args.getOption("--transport") ?: System.getenv("MCP_TRANSPORT") ?: "stdio"
    val port = args.getOption("--port")?.toIntOrNull() ?: System.getenv("MCP_HTTP_PORT")?.toIntOrNull() ?: 3000

    val filteredServer = createServer(baseUrl, draftsimParserBaseUrl)

    when (transport) {
        "stdio" -> runBlocking {
            filteredServer.createSession(
                StdioServerTransport(
                    inputStream = System.`in`.asSource().buffered(),
                    outputStream = System.out.asSink().buffered()
                )
            )
        }
        "http" -> {
            val authIssuerUri = System.getenv("AUTH_ISSUER_URI")
            val mcpBaseUrl = System.getenv("MCP_BASE_URL")

            embeddedServer(CIO, port = port) {
                install(ContentNegotiation) { json(McpJson) }
                install(SSE)

                if (authIssuerUri != null && mcpBaseUrl != null) {
                    install(McpAuthPlugin) {
                        issuerUri = authIssuerUri
                        jwksUri = "$authIssuerUri/oauth2/jwks"
                        resourceMetadataUrl = "$mcpBaseUrl/.well-known/oauth-protected-resource"
                    }
                    intercept(ApplicationCallPipeline.Plugins) {
                        val roles = call.attributes.getOrNull(UserRolesKey)
                        val token = call.attributes.getOrNull(UserTokenKey)
                        if (roles != null && token != null) {
                            withContext(UserRolesElement(roles) + UserTokenElement(token)) {
                                proceed()
                            }
                        } else {
                            proceed()
                        }
                    }
                    routing {
                        oauthMetadataRoutes(mcpBaseUrl, authIssuerUri)
                    }
                }

                val sessionTransports = ConcurrentHashMap<String, StreamableHttpServerTransport>()
                val transportConfig = StreamableHttpServerTransport.Configuration(enableJsonResponse = true)

                routing {
                    route("/mcp") {
                        // SSE GET: resume an existing session
                        sse {
                            val mcpTransport = call.requireSession(sessionTransports) ?: return@sse
                            mcpTransport.handleRequest(this, call)
                        }

                        // POST: initialize a new session or handle an existing one
                        post {
                            val sessionId = call.request.header(MCP_SESSION_ID_HEADER)
                            if (sessionId != null) {
                                val mcpTransport = call.requireSession(sessionTransports) ?: return@post
                                mcpTransport.handleRequest(null, call)
                                return@post
                            }

                            val mcpTransport = StreamableHttpServerTransport(transportConfig)
                            mcpTransport.setOnSessionInitialized { newSessionId ->
                                sessionTransports[newSessionId] = mcpTransport
                            }
                            mcpTransport.setOnSessionClosed { closedSessionId ->
                                sessionTransports.remove(closedSessionId)
                            }
                            filteredServer.createFilteredSession(mcpTransport)
                            mcpTransport.handleRequest(null, call)
                        }

                        // DELETE: close an existing session
                        delete {
                            val mcpTransport = call.requireSession(sessionTransports) ?: return@delete
                            mcpTransport.handleRequest(null, call)
                        }
                    }
                }
            }.start(wait = true)
        }
        else -> {
            System.err.println("Unknown transport: $transport. Use stdio or http.")
            exitProcess(1)
        }
    }
}

private suspend fun ApplicationCall.requireSession(
    sessions: ConcurrentHashMap<String, StreamableHttpServerTransport>,
): StreamableHttpServerTransport? {
    val sessionId = request.header(MCP_SESSION_ID_HEADER)
    if (sessionId.isNullOrEmpty()) {
        respond(HttpStatusCode.BadRequest, "Mcp-Session-Id header is required")
        return null
    }
    val transport = sessions[sessionId]
    if (transport == null) {
        respond(HttpStatusCode.NotFound, "Session not found")
    }
    return transport
}

private fun Array<String>.getOption(flag: String): String? {
    val idx = indexOf(flag)
    return if (idx >= 0 && idx + 1 < size) this[idx + 1] else null
}
