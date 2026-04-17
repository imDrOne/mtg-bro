package xyz.candycrawler.authservice.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserRoleSqlMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JdbcUserRoleRepositoryTest {

    private val mapper = mock<UserRoleSqlMapper>()
    private val repository = JdbcUserRoleRepository(mapper)

    @Test
    fun `assignRole calls mapper with correct userId and role name`() {
        repository.assignRole(42L, UserRole.FREE)

        verify(mapper).insert(42L, "FREE")
    }

    @Test
    fun `findByUserId maps string roles to UserRole enum`() {
        whenever(mapper.selectByUserId(1L)).thenReturn(listOf("FREE", "PRO"))

        val roles = repository.findByUserId(1L)

        assertEquals(listOf(UserRole.FREE, UserRole.PRO), roles)
    }

    @Test
    fun `findByUserId returns empty list when no roles`() {
        whenever(mapper.selectByUserId(99L)).thenReturn(emptyList())

        val roles = repository.findByUserId(99L)

        assertTrue(roles.isEmpty())
    }
}
