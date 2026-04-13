package xyz.candycrawler.authservice.application.rest.dto.request

data class RegisterUserRequest(
    val email: String,
    val username: String,
    val password: String,
)
