package xyz.candycrawler.authservice.domain.refreshtoken.model

import org.junit.jupiter.api.Test
import xyz.candycrawler.authservice.domain.refreshtoken.exception.RefreshTokenInvalidException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshTokenTest {

    private val validHash = "a".repeat(64)
    private val now = Instant.parse("2026-04-25T10:00:00Z")
    private val later = now.plusSeconds(3600)

    @Test
    fun `creates valid token`() {
        val token = RefreshToken(
            id = 1L,
            userId = 42L,
            tokenHash = validHash,
            issuedAt = now,
            expiresAt = later,
            revokedAt = null,
            replacedById = null,
        )
        assertEquals(42L, token.userId)
    }

    @Test
    fun `rejects non-positive userId`() {
        assertFailsWith<RefreshTokenInvalidException> {
            RefreshToken(null, 0L, validHash, now, later, null, null)
        }
    }

    @Test
    fun `rejects blank tokenHash`() {
        assertFailsWith<RefreshTokenInvalidException> {
            RefreshToken(null, 1L, "", now, later, null, null)
        }
    }

    @Test
    fun `rejects tokenHash of wrong length`() {
        assertFailsWith<RefreshTokenInvalidException> {
            RefreshToken(null, 1L, "abc", now, later, null, null)
        }
    }

    @Test
    fun `rejects expiresAt not after issuedAt`() {
        assertFailsWith<RefreshTokenInvalidException> {
            RefreshToken(null, 1L, validHash, now, now, null, null)
        }
    }

    @Test
    fun `isActive returns true for fresh non-revoked token`() {
        val token = RefreshToken(1L, 1L, validHash, now, later, null, null)
        assertTrue(token.isActive(now.plusSeconds(60)))
    }

    @Test
    fun `isActive returns false when revoked`() {
        val token = RefreshToken(1L, 1L, validHash, now, later, now.plusSeconds(10), null)
        assertFalse(token.isActive(now.plusSeconds(60)))
    }

    @Test
    fun `isActive returns false when expired`() {
        val token = RefreshToken(1L, 1L, validHash, now, later, null, null)
        assertFalse(token.isActive(later.plusSeconds(1)))
    }
}
