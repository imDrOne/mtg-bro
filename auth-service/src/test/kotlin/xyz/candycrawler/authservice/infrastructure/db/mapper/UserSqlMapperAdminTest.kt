package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import xyz.candycrawler.common.pagination.PageRequest
import xyz.candycrawler.common.pagination.SortDir
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserSqlMapperAdminTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var userSqlMapper: UserSqlMapper

    private fun insertUser(email: String, username: String, enabled: Boolean = true): UserRecord =
        userSqlMapper.insert(
            UserRecord(null, email, username, "\$2a\$10\$hash", enabled, Instant.now())
        )

    @Test
    fun `update changes enabled flag and persists`() {
        val saved = insertUser("update_test@example.com", "updateuser")
        val updated = userSqlMapper.update(saved.copy(enabled = false))

        assertEquals(false, updated.enabled)
        val fromDb = userSqlMapper.selectById(saved.id!!)
        assertNotNull(fromDb)
        assertEquals(false, fromDb.enabled)
    }

    @Test
    fun `findAll without filter returns all users with pagination`() {
        insertUser("pag1@example.com", "paguser1")
        insertUser("pag2@example.com", "paguser2")

        val page = PageRequest(page = 0, size = 100, sortBy = "createdAt", sortDir = SortDir.DESC)
        val results = userSqlMapper.findAll(UserFilter(null), page)

        assertTrue(results.size >= 2)
    }

    @Test
    fun `findAll with email filter returns only matching users`() {
        insertUser("filter_unique_xq7@example.com", "filteruserxq7")
        insertUser("other_xq7@example.com", "otheruserxq7")

        val page = PageRequest(page = 0, size = 20, sortBy = "email", sortDir = SortDir.ASC)
        val results = userSqlMapper.findAll(UserFilter("filter_unique_xq7"), page)

        assertEquals(1, results.size)
        assertEquals("filter_unique_xq7@example.com", results[0].email)
    }

    @Test
    fun `findAll respects page size`() {
        insertUser("sizepag_a@example.com", "sizepaga")
        insertUser("sizepag_b@example.com", "sizepagb")
        insertUser("sizepag_c@example.com", "sizepagc")

        val page = PageRequest(page = 0, size = 2, sortBy = "email", sortDir = SortDir.ASC)
        val results = userSqlMapper.findAll(UserFilter("sizepag"), page)

        assertEquals(2, results.size)
    }

    @Test
    fun `countAll with email filter counts only matching users`() {
        insertUser("count_xq8@example.com", "countuserxq8")
        insertUser("other_xq8@example.com", "otheruserxq8")

        val count = userSqlMapper.countAll(UserFilter("count_xq8"))

        assertEquals(1L, count)
    }

    @Test
    fun `findAll sorts by email ascending`() {
        insertUser("bbb_sort@example.com", "bbbsortuser")
        insertUser("aaa_sort@example.com", "aaasortuser")

        val page = PageRequest(page = 0, size = 20, sortBy = "email", sortDir = SortDir.ASC)
        val results = userSqlMapper.findAll(UserFilter("_sort@"), page)

        assertTrue(results.size >= 2)
        assertTrue(results[0].email < results[1].email)
    }
}
