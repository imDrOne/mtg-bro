package xyz.candycrawler.authservice.domain.user.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import java.time.Instant
import kotlin.test.assertEquals

class UserTest {

    private fun validUser(
        email: String = "user@example.com",
        username: String = "alice",
        passwordHash: String = "\$2a\$10\$hash",
    ) = User(
        id = null,
        email = email,
        username = username,
        passwordHash = passwordHash,
        enabled = true,
        createdAt = Instant.now(),
    )

    @Test
    fun `valid user constructs successfully`() {
        val user = validUser()
        assertEquals("user@example.com", user.email)
        assertEquals("alice", user.username)
    }

    @Test
    fun `blank email throws UserInvalidException`() {
        assertThrows<UserInvalidException> { validUser(email = "") }
    }

    @Test
    fun `blank email with spaces throws UserInvalidException`() {
        assertThrows<UserInvalidException> { validUser(email = "   ") }
    }

    @Test
    fun `email without at-sign throws UserInvalidException`() {
        assertThrows<UserInvalidException> { validUser(email = "notanemail") }
    }

    @Test
    fun `username shorter than 3 chars throws`() {
        assertThrows<UserInvalidException> { validUser(username = "ab") }
    }

    @Test
    fun `username longer than 50 chars throws`() {
        assertThrows<UserInvalidException> { validUser(username = "a".repeat(51)) }
    }

    @Test
    fun `blank passwordHash throws`() {
        assertThrows<UserInvalidException> { validUser(passwordHash = "") }
    }

    @Test
    fun `id can be null before persistence`() {
        val user = validUser()
        assertEquals(null, user.id)
    }
}
