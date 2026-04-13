package xyz.candycrawler.authservice.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import java.time.Instant

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(email: String, username: String, rawPassword: String): User {
        if (rawPassword.length < 8) throw UserInvalidException("password must be at least 8 characters")

        if (userRepository.existsByEmail(email.lowercase().trim())) {
            throw UserInvalidException("email is already taken")
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw UserInvalidException("username is already taken")
        }

        val user = User(
            id = null,
            email = email.lowercase().trim(),
            username = username.trim(),
            passwordHash = passwordEncoder.encode(rawPassword)!!,
            enabled = true,
            createdAt = Instant.now(),
        )

        return userRepository.save(user)
    }
}
