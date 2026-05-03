package xyz.candycrawler.authservice.application.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import xyz.candycrawler.authservice.domain.refreshtoken.exception.RefreshTokenInvalidException
import xyz.candycrawler.authservice.domain.refreshtoken.model.RefreshToken
import xyz.candycrawler.authservice.domain.refreshtoken.repository.RefreshTokenRepository
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val accessTokenIssuer: AccessTokenIssuer,
    @Value("\${auth.refresh-token.ttl-seconds:2592000}") private val refreshTokenTtlSeconds: Long,
) {

    private val secureRandom = SecureRandom()

    fun login(email: String, rawPassword: String): AuthSession {
        val user = findEnabledUserForLogin(email)
        validatePassword(rawPassword, user)
        return issueSession(user, Instant.now())
    }

    fun refresh(rawRefreshToken: String): AuthSession {
        val now = Instant.now()
        val hash = sha256Hex(rawRefreshToken)
        val stored = findStoredRefreshToken(hash)

        validateRefreshToken(stored, now)

        val user = findRefreshUser(stored)

        val newSession = issueSession(user, now)
        refreshTokenRepository.revoke(stored.id!!, replacedById = newSession.refreshTokenId, revokedAt = now)
        return newSession
    }

    fun logout(rawRefreshToken: String) {
        val hash = sha256Hex(rawRefreshToken)
        val stored = refreshTokenRepository.findByTokenHash(hash) ?: return
        if (stored.revokedAt == null) {
            refreshTokenRepository.revoke(stored.id!!, replacedById = null, revokedAt = Instant.now())
        }
    }

    private fun findEnabledUserForLogin(email: String): User {
        val user = userRepository.findByEmail(email)
            ?: throw UserInvalidException("invalid credentials")
        if (!user.enabled) throw UserInvalidException("invalid credentials")
        return user
    }

    private fun validatePassword(rawPassword: String, user: User) {
        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw UserInvalidException("invalid credentials")
        }
    }

    private fun findStoredRefreshToken(hash: String): RefreshToken = refreshTokenRepository.findByTokenHash(hash)
        ?: throw RefreshTokenInvalidException("refresh token not recognized")

    private fun validateRefreshToken(stored: RefreshToken, now: Instant) {
        if (stored.revokedAt != null) {
            // Reuse of a revoked token: cascade revoke all tokens for this user (theft detection).
            refreshTokenRepository.revokeAllForUser(stored.userId, now)
            throw RefreshTokenInvalidException("refresh token already used")
        }
        if (!stored.isActive(now)) {
            throw RefreshTokenInvalidException("refresh token expired")
        }
    }

    private fun findRefreshUser(stored: RefreshToken): User = userRepository.findById(stored.userId)
        ?: throw UserInvalidException("user not found")

    private fun issueSession(user: User, now: Instant): AuthSession {
        val accessToken = accessTokenIssuer.issue(user, now)
        val rawRefreshToken = generateRefreshTokenValue()
        val saved = refreshTokenRepository.save(
            RefreshToken(
                id = null,
                userId = user.id!!,
                tokenHash = sha256Hex(rawRefreshToken),
                issuedAt = now,
                expiresAt = now.plus(Duration.ofSeconds(refreshTokenTtlSeconds)),
                revokedAt = null,
                replacedById = null,
            ),
        )
        return AuthSession(
            accessToken = accessToken.tokenValue,
            accessTokenExpiresInSeconds = accessToken.expiresInSeconds,
            refreshToken = rawRefreshToken,
            refreshTokenId = saved.id!!,
            refreshTokenExpiresInSeconds = refreshTokenTtlSeconds,
        )
    }

    private fun generateRefreshTokenValue(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class AuthSession(
    val accessToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshToken: String,
    val refreshTokenId: Long,
    val refreshTokenExpiresInSeconds: Long,
)
