package xyz.candycrawler.mcpserver.auth

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun Route.oauthMetadataRoutes(mcpBaseUrl: String, authIssuerUri: String) {
    get("/.well-known/oauth-protected-resource") {
        val metadata = buildJsonObject {
            put("resource", "$mcpBaseUrl/mcp")
            putJsonArray("authorization_servers") { add(authIssuerUri) }
            putJsonArray("scopes_supported") {
                add("openid")
                add("profile")
                add("decks:read")
            }
            putJsonArray("bearer_methods_supported") { add("header") }
        }
        call.respondText(metadata.toString(), ContentType.Application.Json)
    }
}
