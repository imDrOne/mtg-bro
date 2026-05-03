package xyz.candycrawler.authservice.security

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.util.UriComponentsBuilder
import xyz.candycrawler.authservice.application.service.UserRegistrationService
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OAuth2IntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var wac: WebApplicationContext

    @Autowired
    lateinit var registeredClientRepository: RegisteredClientRepository

    @Autowired
    lateinit var userRegistrationService: UserRegistrationService

    lateinit var mockMvc: MockMvc

    // Fresh per test instance (JUnit 5 creates new instance per test method by default)
    private val testClientId = "test-client-${UUID.randomUUID()}"

    // Unique because JdbcRegisteredClientRepository.assertUniqueIdentifiers checks globally.
    private val testClientSecret = "secret-${UUID.randomUUID()}"
    private val testRedirectUri = "http://localhost/callback"

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        val encodedSecret = BCryptPasswordEncoder().encode(testClientSecret)
        val client = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(testClientId)
            .clientSecret(encodedSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(testRedirectUri)
            .scope(OidcScopes.OPENID)
            .scope("profile")
            .clientSettings(
                ClientSettings.builder()
                    .requireProofKey(true)
                    .requireAuthorizationConsent(false)
                    .build(),
            )
            .build()
        registeredClientRepository.save(client)
    }

    // --- Discovery & JWKS ---

    @Test
    fun `OIDC discovery endpoint returns required metadata fields`() {
        mockMvc.get("/.well-known/openid-configuration")
            .andExpect {
                status { isOk() }
                jsonPath("$.issuer") { exists() }
                jsonPath("$.authorization_endpoint") { exists() }
                jsonPath("$.token_endpoint") { exists() }
                jsonPath("$.jwks_uri") { exists() }
                jsonPath("$.response_types_supported") { isArray() }
                jsonPath("$.subject_types_supported") { isArray() }
                jsonPath("$.id_token_signing_alg_values_supported") { isArray() }
            }
    }

    @Test
    fun `JWKS endpoint returns RSA public key`() {
        mockMvc.get("/oauth2/jwks")
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.keys") { isArray() }
                jsonPath("$.keys[0].kty") { value("RSA") }
                jsonPath("$.keys[0].kid") { exists() }
                jsonPath("$.keys[0].n") { exists() }
                jsonPath("$.keys[0].e") { exists() }
            }
    }

    @Test
    fun `JWKS returns stable key_id across multiple requests`() {
        val kid1 = extractKid(mockMvc.get("/oauth2/jwks").andReturn().response.contentAsString)
        val kid2 = extractKid(mockMvc.get("/oauth2/jwks").andReturn().response.contentAsString)
        assertNotNull(kid1)
        assertNotNull(kid2)
        assertTrue(kid1 == kid2, "key_id must be stable — RSA key must be persisted to DB, not regenerated")
    }

    // --- Authorization endpoint ---

    @Test
    fun `unauthenticated authorize request redirects to login`() {
        // OAuth2AuthorizationCodeRequestAuthenticationConverter reads GET params from
        // request.getQueryString(), NOT from the parameter map — pass params in the URL directly.
        val url = authorizeUrl(testClientId, testRedirectUri, "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "openid")
        mockMvc.get(url)
            .andExpect {
                status { is3xxRedirection() }
                header { string("Location", containsString("login")) }
            }
    }

    @Test
    fun `authorize with unknown client_id returns error`() {
        val url =
            authorizeUrl("no-such-client", testRedirectUri, "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "openid")
        mockMvc.get(url)
            .andExpect {
                status { isBadRequest() }
            }
    }

    // --- Login endpoint ---

    @Test
    fun `login with invalid credentials returns 401 JSON`() {
        mockMvc.post("/login") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            param("username", "nobody@example.com")
            param("password", "wrongpassword")
        }.andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.error") { value("Invalid credentials") }
        }
    }

    // --- Token endpoint ---

    @Test
    fun `token endpoint rejects unknown client`() {
        mockMvc.post("/oauth2/token") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            headers { setBasicAuth("no-such-client", "wrong-secret") }
            param("grant_type", "authorization_code")
            param("code", "fake-code")
            param("redirect_uri", testRedirectUri)
            param("code_verifier", "fake-verifier")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `token endpoint rejects wrong client secret`() {
        mockMvc.post("/oauth2/token") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            headers { setBasicAuth(testClientId, "wrong-secret") }
            param("grant_type", "authorization_code")
            param("code", "fake-code")
            param("redirect_uri", testRedirectUri)
            param("code_verifier", "fake-verifier")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // --- Full Authorization Code + PKCE flow ---

    @Test
    fun `full authorization code PKCE flow issues access token and id token`() {
        val email = "pkce-${UUID.randomUUID()}@example.com"
        val username = "pkce${UUID.randomUUID().toString().take(8)}"
        userRegistrationService.register(email, username, "password123")

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = UUID.randomUUID().toString()

        // Step 1: Start authorization — Spring saves pending request in session, redirects to login.
        // Parameters must be in query string (AS reads via getQueryString(), not getParameterMap()).
        val authorizeUrl = authorizeUrl(testClientId, testRedirectUri, codeChallenge, "openid", state)
        val step1 = mockMvc.get(authorizeUrl)
            .andExpect {
                status { is3xxRedirection() }
            }.andReturn()

        val session = step1.request.getSession(false) as? MockHttpSession
        assertNotNull(session, "Spring must create session to preserve pending authorization request")

        // Step 2: POST credentials — Spring authenticates, redirects back to /oauth2/authorize
        val step2 = mockMvc.post("/login") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            param("username", email)
            param("password", "password123")
            this.session = session
        }.andExpect {
            status { is3xxRedirection() }
        }.andReturn()

        val postLoginSession = step2.request.getSession(false) as? MockHttpSession ?: session

        // Step 3: Follow redirect back to /oauth2/authorize — Spring issues auth code,
        //         redirects to client redirect_uri?code=...&state=...
        val authRedirect = step2.response.redirectedUrl
        assertNotNull(authRedirect, "POST /login must redirect after successful authentication")

        val step3 = mockMvc.get(authRedirect) {
            this.session = postLoginSession
        }.andExpect {
            status { is3xxRedirection() }
        }.andReturn()

        val callbackUrl = step3.response.redirectedUrl
        assertNotNull(callbackUrl, "Authorization endpoint must redirect to client callback URI")
        assertTrue(
            callbackUrl.startsWith(testRedirectUri),
            "Redirect must go to registered redirect_uri, got: $callbackUrl",
        )

        val callbackParams = UriComponentsBuilder.fromUriString(callbackUrl).build().queryParams
        val code = callbackParams.getFirst("code")
        val returnedState = callbackParams.getFirst("state")

        assertNotNull(code, "Authorization code must be present in callback URL")
        assertTrue(returnedState == state, "state parameter must be echoed back unchanged")

        // Step 4: Exchange authorization code for tokens (POST uses param map, not query string — works with MockMvc)
        val tokenResponse = mockMvc.post("/oauth2/token") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            headers { setBasicAuth(testClientId, testClientSecret) }
            param("grant_type", "authorization_code")
            param("code", code)
            param("redirect_uri", testRedirectUri)
            param("code_verifier", codeVerifier)
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.access_token") { exists() }
            jsonPath("$.id_token") { exists() }
            jsonPath("$.token_type") { value("Bearer") }
            jsonPath("$.expires_in") { exists() }
        }.andReturn()

        // Verify the access token contains the roles claim with FREE role
        val tokenBody = tokenResponse.response.contentAsString
        val accessToken = Regex("\"access_token\"\\s*:\\s*\"([^\"]+)\"").find(tokenBody)?.groupValues?.get(1)
        assertNotNull(accessToken, "access_token must be present in token response")

        val payloadJson = String(Base64.getUrlDecoder().decode(accessToken.split(".")[1]))
        assertTrue(payloadJson.contains("\"roles\""), "JWT payload must contain roles claim, got: $payloadJson")
        assertTrue(payloadJson.contains("\"FREE\""), "JWT payload must contain FREE role, got: $payloadJson")
    }

    // --- Helpers ---

    /** Builds an authorize URL with all params in the query string.
     *  Required because Spring AS reads GET params via getQueryString(), not getParameterMap(). */
    private fun authorizeUrl(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        scope: String,
        state: String = "test-state",
    ): String = UriComponentsBuilder.fromPath("/oauth2/authorize")
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", scope)
        .queryParam("state", state)
        .queryParam("code_challenge", codeChallenge)
        .queryParam("code_challenge_method", "S256")
        .build()
        .toUriString()

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    private fun extractKid(jwksJson: String): String? =
        Regex("\"kid\"\\s*:\\s*\"([^\"]+)\"").find(jwksJson)?.groupValues?.get(1)
}
