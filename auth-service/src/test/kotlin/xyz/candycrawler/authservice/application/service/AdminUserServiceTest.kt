package xyz.candycrawler.authservice.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import xyz.candycrawler.authservice.domain.refreshtoken.repository.RefreshTokenRepository
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import xyz.candycrawler.common.pagination.PageRequest
import xyz.candycrawler.common.pagination.PageResponse
import xyz.candycrawler.common.pagination.SortDir
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AdminUserServiceTest {

    private val userRepository: UserRepository = mock()
    private val userRoleRepository: UserRoleRepository = mock()
    private val refreshTokenRepository: RefreshTokenRepository = mock()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val service = AdminUserService(userRepository, userRoleRepository, refreshTokenRepository, passwordEncoder)

    private fun user(id: Long, email: String, enabled: Boolean = true) = User(
        id = id,
        email = email,
        username = "user$id",
        passwordHash = "\$2a\$10\$hash",
        enabled = enabled,
        createdAt = Instant.now(),
    )

    @Test
    fun `createAdminUser saves user with ADMIN role`() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByUsername(any())).thenReturn(false)
        whenever(userRepository.save(any())).thenReturn(user(1L, "admin@example.com"))

        val result = service.createAdminUser("admin@example.com", "admin", "password123")

        assertEquals("admin@example.com", result.email)
        verify(userRepository).save(any())
        verify(userRoleRepository).assignRole(1L, UserRole.ADMIN)
    }

    @Test
    fun `createAdminUser throws when email already taken`() {
        whenever(userRepository.existsByEmail("taken@example.com")).thenReturn(true)

        assertThrows<UserInvalidException> {
            service.createAdminUser("taken@example.com", "admin", "password123")
        }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `createAdminUser throws when username already taken`() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
        whenever(userRepository.existsByUsername("takenuser")).thenReturn(true)

        assertThrows<UserInvalidException> {
            service.createAdminUser("admin@example.com", "takenuser", "password123")
        }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `blockUser throws when blocking self`() {
        val email = "admin@example.com"
        whenever(userRepository.findByEmail(email)).thenReturn(user(1L, email))

        assertThrows<UserInvalidException> {
            service.blockUser(1L, email)
        }
        verify(userRepository, never()).update(any<Long>(), any())
    }

    @Test
    fun `blockUser sets enabled false and revokes all tokens`() {
        val existing = user(1L, "target@example.com", enabled = true)
        whenever(userRepository.findByEmail("requester@example.com")).thenReturn(user(99L, "requester@example.com"))
        whenever(userRepository.update(any<Long>(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(User) -> User>(1)
            block(existing)
        }

        service.blockUser(1L, "requester@example.com")

        verify(userRepository).update(any(), any())
        verify(refreshTokenRepository).revokeAllForUser(any(), any())
    }

    @Test
    fun `blockUser results in disabled user`() {
        val existing = user(1L, "target@example.com", enabled = true)
        var capturedUser: User? = null
        whenever(userRepository.findByEmail("requester@example.com")).thenReturn(user(99L, "requester@example.com"))
        whenever(userRepository.update(any<Long>(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(User) -> User>(1)
            val result = block(existing)
            capturedUser = result
            result
        }

        service.blockUser(1L, "requester@example.com")

        assertFalse(capturedUser!!.enabled)
    }

    @Test
    fun `unblockUser sets enabled true`() {
        val existing = user(1L, "target@example.com", enabled = false)
        var capturedUser: User? = null
        whenever(userRepository.update(any<Long>(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(User) -> User>(1)
            val result = block(existing)
            capturedUser = result
            result
        }

        service.unblockUser(1L)

        assertEquals(true, capturedUser!!.enabled)
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any())
    }

    @Test
    fun `listUsers delegates to repository and maps to responses`() {
        val filter = UserFilter(null)
        val page = PageRequest(0, 20, "createdAt", SortDir.DESC)
        val pageResponse = PageResponse(
            items = listOf(user(1L, "a@example.com"), user(2L, "b@example.com")),
            page = 0,
            size = 20,
            totalItems = 2L,
            totalPages = 1,
        )
        whenever(userRepository.findAll(filter, page)).thenReturn(pageResponse)

        val result = service.listUsers(filter, page)

        assertEquals(2, result.items.size)
        assertEquals(2L, result.totalItems)
    }
}
