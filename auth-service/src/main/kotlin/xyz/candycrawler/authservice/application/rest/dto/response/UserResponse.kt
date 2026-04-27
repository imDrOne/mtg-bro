package xyz.candycrawler.authservice.application.rest.dto.response

import java.time.Instant

data class UserResponse(
    val id: Long,
    val email: String,
    val username: String,
    val enabled: Boolean,
    val createdAt: Instant,
)
