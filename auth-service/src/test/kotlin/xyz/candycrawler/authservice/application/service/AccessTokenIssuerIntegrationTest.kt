package xyz.candycrawler.authservice.application.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.application.rest.AuthController
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import com.nimbusds.jwt.SignedJWT
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccessTokenIssuerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var accessTokenIssuer: AccessTokenIssuer
    @Autowired lateinit var registrationService: UserRegistrationService

    @Test
    fun `issued JWT contains roles and permissions claims`() {
        val user = registrationService.register("issuer-test@example.com", "issueruser", "password123")

        val token = accessTokenIssuer.issue(user)

        val jwt = SignedJWT.parse(token.tokenValue)
        val claims = jwt.jwtClaimsSet

        val roles = claims.getStringListClaim("roles")
        assertNotNull(roles)
        assertTrue(roles.isNotEmpty(), "roles claim must not be empty")

        val permissions = claims.getStringListClaim("permissions")
        assertNotNull(permissions)
        assertTrue(permissions.isNotEmpty(), "permissions claim must not be empty")
    }

    @Test
    fun `issued JWT has correct subject and issuer`() {
        val user = registrationService.register("issuer-sub@example.com", "issuersubuser", "password123")

        val token = accessTokenIssuer.issue(user)

        val jwt = SignedJWT.parse(token.tokenValue)
        val claims = jwt.jwtClaimsSet

        assertTrue(claims.subject == user.email)
        assertTrue(claims.issuer.isNotBlank())
    }
}
