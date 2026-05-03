package xyz.candycrawler.mcpserver.auth

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

val McpAuthPlugin = createApplicationPlugin("McpAuth", ::McpAuthConfig) {
    val log = LoggerFactory.getLogger("McpAuthPlugin")
    val issuerUri = pluginConfig.issuerUri
    val resourceMetadataUrl = pluginConfig.resourceMetadataUrl
    val scopes = pluginConfig.scopes

    val jwkProvider = JwkProviderBuilder(URI(pluginConfig.jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    onCall { call ->
        val path = call.request.path()
        if (path.startsWith("/.well-known")) return@onCall

        val authHeader = call.request.headers["Authorization"]
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.response.header(
                "WWW-Authenticate",
                """Bearer resource_metadata="$resourceMetadataUrl", scope="$scopes"""",
            )
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        try {
            val decodedJwt = JWT.decode(token)
            val jwk = jwkProvider.get(decodedJwt.keyId)
            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
            JWT.require(algorithm)
                .withIssuer(issuerUri)
                .build()
                .verify(decodedJwt)
            val roles = decodedJwt.getClaim("roles").asList(String::class.java) ?: emptyList()
            call.attributes.put(UserRolesKey, roles)
            call.attributes.put(UserTokenKey, token)
        } catch (e: JWTVerificationException) {
            rejectInvalidBearerToken(call, resourceMetadataUrl, log, e)
        } catch (e: JwkException) {
            rejectInvalidBearerToken(call, resourceMetadataUrl, log, e)
        } catch (e: ClassCastException) {
            rejectInvalidBearerToken(call, resourceMetadataUrl, log, e)
        }
    }
}

private suspend fun rejectInvalidBearerToken(
    call: ApplicationCall,
    resourceMetadataUrl: String,
    log: Logger,
    cause: Throwable,
) {
    log.debug("Rejected invalid MCP bearer token", cause)
    call.response.header(
        "WWW-Authenticate",
        """Bearer error="invalid_token", resource_metadata="$resourceMetadataUrl"""",
    )
    call.respond(HttpStatusCode.Unauthorized)
}
