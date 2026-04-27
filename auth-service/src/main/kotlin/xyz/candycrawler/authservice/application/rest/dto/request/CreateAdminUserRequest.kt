package xyz.candycrawler.authservice.application.rest.dto.request

data class CreateAdminUserRequest(
    val email: String,
    val username: String,
    val password: String,
)
