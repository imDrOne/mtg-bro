package xyz.candycrawler.authservice.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import java.time.Instant

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(email: String, username: String, rawPassword: String): User {
        val normalizedEmail = email.lowercase().trim()
        val trimmedUsername = username.trim()

        validatePassword(rawPassword)
        validateUniqueUser(normalizedEmail, trimmedUsername)

        val user = User(
            id = null,
            email = normalizedEmail,
            username = trimmedUsername,
            passwordHash = passwordEncoder.encode(rawPassword)!!,
            enabled = true,
            createdAt = Instant.now(),
        )

        val saved = userRepository.save(user)
        userRoleRepository.assignRole(
            requireNotNull(saved.id) {
                "saved user must have a database-assigned id"
            },
            UserRole.FREE,
        )
        return saved
    }

    private fun validatePassword(rawPassword: String) {
        if (rawPassword.length < MIN_PASSWORD_LENGTH) {
            throw UserInvalidException("password must be at least $MIN_PASSWORD_LENGTH characters")
        }
    }

    private fun validateUniqueUser(normalizedEmail: String, trimmedUsername: String) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw UserInvalidException("email is already taken")
        }
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw UserInvalidException("username is already taken")
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
