package xyz.candycrawler.authservice.infrastructure.db.entity

import java.time.Instant

data class UserRoleRecord(
    val userId: Long,
    val role: String,
    val grantedAt: Instant,
)
