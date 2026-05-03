package xyz.candycrawler.authservice.domain.refreshtoken.model

import xyz.candycrawler.authservice.domain.refreshtoken.exception.RefreshTokenInvalidException
import java.time.Instant

class RefreshToken(
    val id: Long?,
    val userId: Long,
    val tokenHash: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val replacedById: Long?,
) {
    init {
        if (userId <= 0) throw RefreshTokenInvalidException("userId must be positive")
        if (tokenHash.isBlank()) throw RefreshTokenInvalidException("tokenHash must not be blank")
        if (tokenHash.length != 64) throw RefreshTokenInvalidException("tokenHash must be SHA-256 hex (64 chars)")
        if (!expiresAt.isAfter(issuedAt)) throw RefreshTokenInvalidException("expiresAt must be after issuedAt")
    }

    fun isActive(now: Instant): Boolean = revokedAt == null && now.isBefore(expiresAt)
}
