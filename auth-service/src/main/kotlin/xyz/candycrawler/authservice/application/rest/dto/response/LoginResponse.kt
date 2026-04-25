package xyz.candycrawler.authservice.application.rest.dto.response

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)
