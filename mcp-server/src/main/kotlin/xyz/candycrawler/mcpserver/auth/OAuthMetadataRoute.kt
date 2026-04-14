package xyz.candycrawler.mcpserver.auth

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.oauthMetadataRoutes(mcpBaseUrl: String, authIssuerUri: String) {
    get("/.well-known/oauth-protected-resource") {
        call.respond(
            mapOf(
                "resource" to "$mcpBaseUrl/mcp",
                "authorization_servers" to listOf(authIssuerUri),
                "scopes_supported" to listOf("openid", "profile", "decks:read"),
                "bearer_methods_supported" to listOf("header"),
            )
        )
    }
}
