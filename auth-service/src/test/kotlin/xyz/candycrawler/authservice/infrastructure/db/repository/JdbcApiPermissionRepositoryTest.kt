package xyz.candycrawler.authservice.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.infrastructure.db.entity.ApiPermissionRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.ApiPermissionSqlMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JdbcApiPermissionRepositoryTest {

    private val sqlMapper: ApiPermissionSqlMapper = mock()
    private val repository = JdbcApiPermissionRepository(sqlMapper)

    private val record = ApiPermissionRecord(1L, "cards:read", "Read cards")

    @Test
    fun `findByRole delegates to mapper with role name and maps to domain`() {
        whenever(sqlMapper.selectByRoles(listOf("FREE"))).thenReturn(listOf(record))

        val result = repository.findByRole(UserRole.FREE)

        assertEquals(1, result.size)
        assertEquals("cards:read", result[0].name)
        assertEquals("Read cards", result[0].description)
    }

    @Test
    fun `findByRoles passes all role names and returns merged list`() {
        whenever(sqlMapper.selectByRoles(listOf("FREE", "PRO"))).thenReturn(listOf(record))

        val result = repository.findByRoles(listOf(UserRole.FREE, UserRole.PRO))

        assertEquals(1, result.size)
    }

    @Test
    fun `findByRoles returns empty list when mapper returns empty`() {
        whenever(sqlMapper.selectByRoles(listOf("ADMIN"))).thenReturn(emptyList())

        val result = repository.findByRole(UserRole.ADMIN)

        assertTrue(result.isEmpty())
    }
}
