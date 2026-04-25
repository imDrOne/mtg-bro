package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.ApiPermissionSqlMapper
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiPermissionSqlMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mapper: ApiPermissionSqlMapper

    @Test
    fun `selectByRoles returns permissions for known role`() {
        val results = mapper.selectByRoles(listOf("FREE"))
        assertTrue(results.isNotEmpty(), "Expected at least one permission for ROLE_USER")
        results.forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.description.isNotBlank())
        }
    }

    @Test
    fun `selectByRoles deduplicates when multiple roles share a permission`() {
        val single = mapper.selectByRoles(listOf("FREE"))
        val multi = mapper.selectByRoles(listOf("FREE", "ADMIN"))
        // ROLE_ADMIN should have at least as many permissions as ROLE_USER
        assertTrue(multi.size >= single.size)
        val names = multi.map { it.name }
        assertEquals(names.distinct(), names, "Result should have no duplicates")
    }

    @Test
    fun `selectByRoles returns empty list for empty input`() {
        val results = mapper.selectByRoles(emptyList())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `selectByRoles returns empty list for unknown role`() {
        val results = mapper.selectByRoles(listOf("ROLE_NONEXISTENT"))
        assertTrue(results.isEmpty())
    }
}
