package xyz.candycrawler.authservice.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRegistrationServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val service = UserRegistrationService(userRepository, passwordEncoder)

    private fun savedUser(email: String, username: String, hash: String) = User(
        id = 1L, email = email, username = username, passwordHash = hash,
        enabled = true, createdAt = Instant.now(),
    )

    @Test
    fun `register saves user with encoded password`() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByUsername(any())).thenReturn(false)
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            val user = invocation.getArgument<User>(0)
            savedUser(user.email, user.username, user.passwordHash)
        }

        val result = service.register("test@example.com", "testuser", "password123")

        assertEquals("test@example.com", result.email)
        assertTrue(passwordEncoder.matches("password123", result.passwordHash))
        verify(userRepository).save(any())
    }

    @Test
    fun `register normalizes email to lowercase`() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByUsername(any())).thenReturn(false)
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            val user = invocation.getArgument<User>(0)
            savedUser(user.email, user.username, user.passwordHash)
        }

        val result = service.register("Test@Example.COM", "testuser2", "password123")
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun `register throws when password is too short`() {
        assertThrows<UserInvalidException> {
            service.register("test@example.com", "testuser", "short")
        }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `register throws when email is already taken`() {
        whenever(userRepository.existsByEmail("taken@example.com")).thenReturn(true)

        assertThrows<UserInvalidException> {
            service.register("taken@example.com", "newuser", "password123")
        }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `register throws when username is already taken`() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByUsername("takenuser")).thenReturn(true)

        assertThrows<UserInvalidException> {
            service.register("new@example.com", "takenuser", "password123")
        }
        verify(userRepository, never()).save(any())
    }
}
