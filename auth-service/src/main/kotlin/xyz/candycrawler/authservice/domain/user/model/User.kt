package xyz.candycrawler.authservice.domain.user.model

import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import java.time.Instant

class User(
    val id: Long?,
    val email: String,
    val username: String,
    val passwordHash: String,
    val enabled: Boolean,
    val createdAt: Instant,
) {
    init {
        if (email.isBlank()) throw UserInvalidException("email must not be blank")
        if (!email.contains('@')) throw UserInvalidException("email must be a valid email address")
        if (username.length !in 3..50) throw UserInvalidException("username must be 3–50 characters")
        if (passwordHash.isBlank()) throw UserInvalidException("passwordHash must not be blank")
    }
}
