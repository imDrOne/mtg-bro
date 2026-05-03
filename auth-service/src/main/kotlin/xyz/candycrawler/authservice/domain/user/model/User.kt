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
        if (username.length !in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH) {
            throw UserInvalidException("username must be $MIN_USERNAME_LENGTH–$MAX_USERNAME_LENGTH characters")
        }
        if (passwordHash.isBlank()) throw UserInvalidException("passwordHash must not be blank")
    }

    private companion object {
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 50
    }
}
