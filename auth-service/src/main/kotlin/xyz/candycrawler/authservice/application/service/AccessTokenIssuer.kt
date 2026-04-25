package xyz.candycrawler.authservice.application.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import xyz.candycrawler.authservice.domain.permission.repository.ApiPermissionRepository
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import java.time.Duration
import java.time.Instant

@Service
class AccessTokenIssuer(
    private val jwtEncoder: JwtEncoder,
    private val userRoleRepository: UserRoleRepository,
    private val apiPermissionRepository: ApiPermissionRepository,
    @Value("\${auth.issuer-uri}") private val issuerUri: String,
    @Value("\${auth.access-token.ttl-seconds:900}") private val accessTokenTtlSeconds: Long,
) {

    fun issue(user: User, now: Instant = Instant.now()): IssuedAccessToken {
        val userId = requireNotNull(user.id) { "user.id is required" }
        val roles = userRoleRepository.findByUserId(userId)
        val permissions = apiPermissionRepository.findByRoles(roles)
        val expiresAt = now.plus(Duration.ofSeconds(accessTokenTtlSeconds))

        val claims = JwtClaimsSet.builder()
            .issuer(issuerUri)
            .subject(user.email)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("roles", roles.map { it.name })
            .claim("permissions", permissions.map { it.name })
            .build()

        val header = JwsHeader.with(SignatureAlgorithm.RS256).build()
        val tokenValue = jwtEncoder
            .encode(JwtEncoderParameters.from(header, claims))
            .tokenValue
        return IssuedAccessToken(tokenValue, accessTokenTtlSeconds)
    }
}

data class IssuedAccessToken(val tokenValue: String, val expiresInSeconds: Long)
