package xyz.candycrawler.authservice.infrastructure.db.entity

import java.time.Instant

data class UserRecord(
    val id: Long?,
    val email: String,
    val username: String,
    val passwordHash: String,
    val enabled: Boolean,
    val createdAt: Instant,
)
