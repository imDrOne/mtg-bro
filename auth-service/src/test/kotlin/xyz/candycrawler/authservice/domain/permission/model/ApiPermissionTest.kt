package xyz.candycrawler.authservice.domain.permission.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ApiPermissionTest {

    @Test
    fun `creates valid permission`() {
        val perm = ApiPermission(1L, "cards:read", "Read access to cards")
        assertEquals("cards:read", perm.name)
    }

    @Test
    fun `rejects blank name`() {
        assertThrows<IllegalArgumentException> { ApiPermission(1L, " ", "desc") }
    }

    @Test
    fun `rejects blank description`() {
        assertThrows<IllegalArgumentException> { ApiPermission(1L, "cards:read", "") }
    }
}
