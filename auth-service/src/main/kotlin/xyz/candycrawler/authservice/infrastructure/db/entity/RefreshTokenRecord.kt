package xyz.candycrawler.authservice.infrastructure.db.entity

import java.time.Instant

data class RefreshTokenRecord(
    val id: Long?,
    val userId: Long,
    val tokenHash: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val replacedById: Long?,
)
