package xyz.candycrawler.mcpserver

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import xyz.candycrawler.mcpserver.auth.McpAuthPlugin
import xyz.candycrawler.mcpserver.auth.oauthMetadataRoutes
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val baseUrl = System.getenv("COLLECTION_MANAGER_BASE_URL") ?: "http://localhost:8080"
    val draftsimParserBaseUrl = System.getenv("DRAFTSIM_PARSER_BASE_URL") ?: "http://localhost:8081"
    val transport = args.getOption("--transport") ?: System.getenv("MCP_TRANSPORT") ?: "stdio"
    val port = args.getOption("--port")?.toIntOrNull() ?: System.getenv("MCP_HTTP_PORT")?.toIntOrNull() ?: 3000

    val server = createServer(baseUrl, draftsimParserBaseUrl)

    when (transport) {
        "stdio" -> runBlocking {
            server.createSession(
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

                if (authIssuerUri != null && mcpBaseUrl != null) {
                    install(McpAuthPlugin) {
                        issuerUri = authIssuerUri
                        jwksUri = "$authIssuerUri/oauth2/jwks"
                        resourceMetadataUrl = "$mcpBaseUrl/.well-known/oauth-protected-resource"
                    }
                    routing {
                        oauthMetadataRoutes(mcpBaseUrl, authIssuerUri)
                    }
                }

                mcpStreamableHttp { server }
            }.start(wait = true)
        }
        else -> {
            System.err.println("Unknown transport: $transport. Use stdio or http.")
            exitProcess(1)
        }
    }
}

private fun Array<String>.getOption(flag: String): String? {
    val idx = indexOf(flag)
    return if (idx >= 0 && idx + 1 < size) this[idx + 1] else null
}
