package xyz.candycrawler.authservice.application.rest.dto.request

data class LoginRequest(
    val email: String,
    val password: String,
)
