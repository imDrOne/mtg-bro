package xyz.candycrawler.authservice.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Service
import xyz.candycrawler.authservice.application.rest.dto.request.DynamicClientRegistrationRequest
import xyz.candycrawler.authservice.application.rest.dto.response.DynamicClientRegistrationResponse
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class DynamicClientRegistrationService(
    private val registeredClientRepository: RegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun register(request: DynamicClientRegistrationRequest): DynamicClientRegistrationResponse {
        validateRedirectUris(request.redirectUris)

        val clientId = UUID.randomUUID().toString()
        val clientName = request.clientName ?: "dynamic-client-$clientId"
        val authMethod = resolveAuthMethod(request.tokenEndpointAuthMethod)
        val isConfidential = authMethod != ClientAuthenticationMethod.NONE

        val rawSecret: String? = if (isConfidential) generateSecret() else null
        val encodedSecret: String? = rawSecret?.let { passwordEncoder.encode(it) }
        val now = Instant.now()

        val builder = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientIdIssuedAt(now)
            .clientName(clientName)
            .clientAuthenticationMethod(authMethod)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .scope(OidcScopes.OPENID)
            .scope("profile")
            .scope("decks:read")
            .clientSettings(
                ClientSettings.builder()
                    .requireProofKey(true)
                    .requireAuthorizationConsent(false)
                    .build()
            )
            .tokenSettings(
                TokenSettings.builder()
                    .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .refreshTokenTimeToLive(Duration.ofDays(30))
                    .reuseRefreshTokens(false)
                    .build()
            )

        request.redirectUris.forEach { builder.redirectUri(it) }
        if (encodedSecret != null) builder.clientSecret(encodedSecret)

        registeredClientRepository.save(builder.build())

        return DynamicClientRegistrationResponse(
            clientId = clientId,
            clientSecret = rawSecret,
            clientIdIssuedAt = now.epochSecond,
            clientSecretExpiresAt = if (isConfidential) 0 else null,
            redirectUris = request.redirectUris,
            clientName = clientName,
            tokenEndpointAuthMethod = authMethod.value,
            grantTypes = listOf("authorization_code", "refresh_token"),
            responseTypes = listOf("code"),
            scope = "openid profile decks:read",
        )
    }

    private fun resolveAuthMethod(requested: String?): ClientAuthenticationMethod =
        when (requested) {
            "client_secret_basic" -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC
            "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST
            "none", null -> ClientAuthenticationMethod.NONE
            else -> throw IllegalArgumentException(
                "Unsupported token_endpoint_auth_method: $requested"
            )
        }

    private fun validateRedirectUris(uris: List<String>) {
        require(uris.isNotEmpty()) { "redirect_uris must not be empty" }
        uris.forEach { uri ->
            require(uri.startsWith("https://") || uri.startsWith("http://localhost")) {
                "redirect_uri must use HTTPS (except localhost): $uri"
            }
        }
    }

    private fun generateSecret(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
