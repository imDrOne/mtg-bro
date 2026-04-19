package xyz.candycrawler.authservice.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import xyz.candycrawler.authservice.application.rest.dto.request.DynamicClientRegistrationRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicClientRegistrationServiceTest {

    private val registeredClientRepository: RegisteredClientRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val service = DynamicClientRegistrationService(registeredClientRepository, passwordEncoder)

    @Test
    fun `register with default auth method creates confidential client`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$encoded")

        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://claude.ai/api/mcp/auth_callback"),
        )

        val response = service.register(request)

        assertEquals("client_secret_basic", response.tokenEndpointAuthMethod)
        assertNotNull(response.clientSecret)
        assertNotNull(response.clientId)
    }

    @Test
    fun `register with none auth method creates public client`() {
        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://claude.ai/api/mcp/auth_callback"),
            tokenEndpointAuthMethod = "none",
        )

        val response = service.register(request)

        assertEquals("none", response.tokenEndpointAuthMethod)
        assertNull(response.clientSecret)
    }

    @Test
    fun `register with client_secret_basic creates confidential client with secret`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$encoded")

        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://example.com/callback"),
            tokenEndpointAuthMethod = "client_secret_basic",
        )

        val response = service.register(request)

        assertEquals("client_secret_basic", response.tokenEndpointAuthMethod)
        assertNotNull(response.clientSecret)
        assertEquals(0L, response.clientSecretExpiresAt)
    }

    @Test
    fun `register with client_secret_post creates confidential client`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$encoded")

        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://example.com/callback"),
            tokenEndpointAuthMethod = "client_secret_post",
        )

        val response = service.register(request)

        assertEquals("client_secret_post", response.tokenEndpointAuthMethod)
        assertNotNull(response.clientSecret)
    }

    @Test
    fun `register always assigns fixed scopes`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$encoded")

        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://example.com/callback"),
            scope = "admin everything",
        )

        val response = service.register(request)

        assertEquals("openid profile decks:read", response.scope)
    }

    @Test
    fun `register throws on empty redirect_uris`() {
        val request = DynamicClientRegistrationRequest(redirectUris = emptyList())

        assertThrows<IllegalArgumentException> { service.register(request) }
    }

    @Test
    fun `register throws on non-HTTPS redirect_uri`() {
        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("http://example.com/callback"),
        )

        assertThrows<IllegalArgumentException> { service.register(request) }
    }

    @Test
    fun `register allows http localhost redirect_uri`() {
        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("http://localhost:3000/callback"),
        )

        val response = service.register(request)

        assertEquals(listOf("http://localhost:3000/callback"), response.redirectUris)
    }

    @Test
    fun `register throws on unsupported auth method`() {
        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://example.com/callback"),
            tokenEndpointAuthMethod = "private_key_jwt",
        )

        assertThrows<IllegalArgumentException> { service.register(request) }
    }

    @Test
    fun `register saves client with PKCE required`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$encoded")
        val captor = argumentCaptor<RegisteredClient>()

        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://example.com/callback"),
        )

        service.register(request)

        verify(registeredClientRepository).save(captor.capture())
        assertTrue(captor.firstValue.clientSettings.isRequireProofKey)
    }

    @Test
    fun `register returns grant_types authorization_code and refresh_token`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$encoded")

        val request = DynamicClientRegistrationRequest(
            redirectUris = listOf("https://example.com/callback"),
        )

        val response = service.register(request)

        assertEquals(listOf("authorization_code", "refresh_token"), response.grantTypes)
        assertEquals(listOf("code"), response.responseTypes)
    }
}
