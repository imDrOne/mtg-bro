package xyz.candycrawler.authservice.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import xyz.candycrawler.authservice.domain.refreshtoken.exception.RefreshTokenInvalidException
import xyz.candycrawler.authservice.domain.refreshtoken.model.RefreshToken
import xyz.candycrawler.authservice.domain.refreshtoken.repository.RefreshTokenRepository
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthenticationServiceTest {

    private val userRepository: UserRepository = mock()
    private val refreshTokenRepository: RefreshTokenRepository = mock()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val accessTokenIssuer: AccessTokenIssuer = mock()
    private val service = AuthenticationService(
        userRepository,
        refreshTokenRepository,
        passwordEncoder,
        accessTokenIssuer,
        refreshTokenTtlSeconds = 3600,
    )

    private val rawPassword = "secret123"
    private val passwordHash = passwordEncoder.encode(rawPassword)!!
    private val activeUser = User(
        id = 7L, email = "user@example.com", username = "user",
        passwordHash = passwordHash, enabled = true, createdAt = Instant.now(),
    )

    private fun mockIssue() {
        whenever(accessTokenIssuer.issue(any(), any())).thenReturn(IssuedAccessToken("access-jwt", 900))
    }

    private fun mockSave() {
        whenever(refreshTokenRepository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<RefreshToken>(0)
            RefreshToken(
                id = 1001L,
                userId = arg.userId,
                tokenHash = arg.tokenHash,
                issuedAt = arg.issuedAt,
                expiresAt = arg.expiresAt,
                revokedAt = arg.revokedAt,
                replacedById = arg.replacedById,
            )
        }
    }

    @Test
    fun `login returns session and persists hashed refresh token`() {
        whenever(userRepository.findByEmail("user@example.com")).thenReturn(activeUser)
        mockIssue()
        mockSave()

        val session = service.login("user@example.com", rawPassword)

        assertEquals("access-jwt", session.accessToken)
        assertNotNull(session.refreshToken)
        val captor = argumentCaptor<RefreshToken>()
        verify(refreshTokenRepository).save(captor.capture())
        assertEquals(sha256Hex(session.refreshToken), captor.firstValue.tokenHash)
    }

    @Test
    fun `login throws on unknown email`() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)
        assertThrows<UserInvalidException> { service.login("ghost@example.com", "pw") }
    }

    @Test
    fun `login throws on disabled user`() {
        val disabled = User(8L, "x@x", "xxx", passwordHash, false, Instant.now())
        whenever(userRepository.findByEmail(any())).thenReturn(disabled)
        assertThrows<UserInvalidException> { service.login("x@x", rawPassword) }
    }

    @Test
    fun `login throws on bad password`() {
        whenever(userRepository.findByEmail(any())).thenReturn(activeUser)
        assertThrows<UserInvalidException> { service.login("user@example.com", "wrong") }
    }

    @Test
    fun `refresh rotates token and revokes old`() {
        val raw = "raw-refresh"
        val hash = sha256Hex(raw)
        val now = Instant.now()
        val stored = RefreshToken(
            id = 500L, userId = 7L, tokenHash = hash,
            issuedAt = now.minusSeconds(60), expiresAt = now.plusSeconds(3600),
            revokedAt = null, replacedById = null,
        )
        whenever(refreshTokenRepository.findByTokenHash(hash)).thenReturn(stored)
        whenever(userRepository.findById(7L)).thenReturn(activeUser)
        mockIssue()
        mockSave()

        val session = service.refresh(raw)

        assertEquals("access-jwt", session.accessToken)
        verify(refreshTokenRepository).revoke(eq(500L), eq(1001L), any())
    }

    @Test
    fun `refresh on revoked token cascades revoke for user and throws`() {
        val raw = "stolen"
        val hash = sha256Hex(raw)
        val now = Instant.now()
        val revoked = RefreshToken(
            id = 600L, userId = 7L, tokenHash = hash,
            issuedAt = now.minusSeconds(120), expiresAt = now.plusSeconds(3600),
            revokedAt = now.minusSeconds(60), replacedById = 601L,
        )
        whenever(refreshTokenRepository.findByTokenHash(hash)).thenReturn(revoked)

        assertThrows<RefreshTokenInvalidException> { service.refresh(raw) }
        verify(refreshTokenRepository).revokeAllForUser(eq(7L), any())
        verify(refreshTokenRepository, never()).save(any())
    }

    @Test
    fun `refresh throws on unknown token without cascade`() {
        whenever(refreshTokenRepository.findByTokenHash(any())).thenReturn(null)
        assertThrows<RefreshTokenInvalidException> { service.refresh("nope") }
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any())
    }

    @Test
    fun `refresh throws on expired token`() {
        val raw = "expired"
        val hash = sha256Hex(raw)
        val now = Instant.now()
        val expired = RefreshToken(
            id = 700L, userId = 7L, tokenHash = hash,
            issuedAt = now.minusSeconds(7200), expiresAt = now.minusSeconds(60),
            revokedAt = null, replacedById = null,
        )
        whenever(refreshTokenRepository.findByTokenHash(hash)).thenReturn(expired)
        assertThrows<RefreshTokenInvalidException> { service.refresh(raw) }
    }

    @Test
    fun `logout revokes token`() {
        val raw = "to-logout"
        val hash = sha256Hex(raw)
        val now = Instant.now()
        val stored = RefreshToken(
            id = 800L, userId = 7L, tokenHash = hash,
            issuedAt = now.minusSeconds(60), expiresAt = now.plusSeconds(3600),
            revokedAt = null, replacedById = null,
        )
        whenever(refreshTokenRepository.findByTokenHash(hash)).thenReturn(stored)

        service.logout(raw)

        verify(refreshTokenRepository).revoke(eq(800L), eq(null), any())
    }

    @Test
    fun `logout is idempotent on unknown or already-revoked token`() {
        whenever(refreshTokenRepository.findByTokenHash(any())).thenReturn(null)
        service.logout("nothing")
        verify(refreshTokenRepository, never()).revoke(any(), any(), any())
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
