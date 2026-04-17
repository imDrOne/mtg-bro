package xyz.candycrawler.authservice.domain.user.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoleTest {

    @Test
    fun `FREE is the default role`() {
        assertEquals("FREE", UserRole.FREE.name)
    }

    @Test
    fun `all roles are defined`() {
        val roles = UserRole.entries.map { it.name }
        assertEquals(listOf("FREE", "PRO", "ADMIN"), roles)
    }
}
