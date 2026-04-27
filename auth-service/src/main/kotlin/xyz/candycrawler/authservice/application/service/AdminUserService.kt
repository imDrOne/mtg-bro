package xyz.candycrawler.authservice.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.refreshtoken.repository.RefreshTokenRepository
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import xyz.candycrawler.common.pagination.PageRequest
import xyz.candycrawler.common.pagination.PageResponse
import java.time.Instant

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun createAdminUser(email: String, username: String, rawPassword: String): User {
        if (rawPassword.length < 8) throw UserInvalidException("password must be at least 8 characters")

        val normalizedEmail = email.lowercase().trim()
        val trimmedUsername = username.trim()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw UserInvalidException("email is already taken")
        }
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw UserInvalidException("username is already taken")
        }

        val user = User(
            id = null,
            email = normalizedEmail,
            username = trimmedUsername,
            passwordHash = passwordEncoder.encode(rawPassword)!!,
            enabled = true,
            createdAt = Instant.now(),
        )

        val saved = userRepository.save(user)
        userRoleRepository.assignRole(requireNotNull(saved.id), UserRole.ADMIN)
        return saved
    }

    @Transactional
    fun blockUser(id: Long, currentUserEmail: String) {
        val currentUserId = userRepository.findByEmail(currentUserEmail)?.id
            ?: throw UserInvalidException("current user not found")
        if (id == currentUserId) throw UserInvalidException("cannot block yourself")
        userRepository.update(id) { u ->
            User(u.id, u.email, u.username, u.passwordHash, enabled = false, u.createdAt)
        }
        refreshTokenRepository.revokeAllForUser(id, Instant.now())
    }

    @Transactional
    fun unblockUser(id: Long) {
        userRepository.update(id) { u ->
            User(u.id, u.email, u.username, u.passwordHash, enabled = true, u.createdAt)
        }
    }

    @Transactional(readOnly = true)
    fun listUsers(filter: UserFilter, page: PageRequest): PageResponse<User> =
        userRepository.findAll(filter, page)
}
