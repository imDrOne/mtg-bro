package xyz.candycrawler.authservice.application.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import java.time.Instant

class UserRegistrationServiceRoleTest {

    private val userRepository = mock<UserRepository>()
    private val userRoleRepository = mock<UserRoleRepository>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val service = UserRegistrationService(userRepository, userRoleRepository, passwordEncoder)

    @Test
    fun `register assigns FREE role to new user`() {
        whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$hash")
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByUsername(any())).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(
            User(
                id = 1L,
                email = "new@example.com",
                username = "newuser",
                passwordHash = "\$2a\$10\$hash",
                enabled = true,
                createdAt = Instant.now(),
            )
        )

        service.register("new@example.com", "newuser", "password123")

        verify(userRoleRepository).assignRole(1L, UserRole.FREE)
    }
}
