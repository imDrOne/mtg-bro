package xyz.candycrawler.authservice.application.rest.dto.response

import java.time.Instant

data class RegisterUserResponse(
    val id: Long,
    val email: String,
    val username: String,
    val createdAt: Instant,
)
