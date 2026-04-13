package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserSqlMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var userSqlMapper: UserSqlMapper

    @Test
    fun `insert returns record with generated id and created_at`() {
        val record = UserRecord(
            id = null,
            email = "mapper_test@example.com",
            username = "mappertest",
            passwordHash = "\$2a\$10\$hash",
            enabled = true,
            createdAt = Instant.now(),
        )
        val saved = userSqlMapper.insert(record)

        assertNotNull(saved.id)
        assertTrue(saved.id!! > 0)
        assertEquals("mapper_test@example.com", saved.email)
        assertEquals("mappertest", saved.username)
    }

    @Test
    fun `selectById returns inserted record`() {
        val record = UserRecord(null, "byid@example.com", "byiduser", "\$2a\$10\$hash", true, Instant.now())
        val saved = userSqlMapper.insert(record)

        val found = userSqlMapper.selectById(saved.id!!)
        assertNotNull(found)
        assertEquals("byid@example.com", found.email)
    }

    @Test
    fun `selectById returns null for unknown id`() {
        assertNull(userSqlMapper.selectById(Long.MAX_VALUE))
    }

    @Test
    fun `selectByEmail returns inserted record`() {
        val email = "byemail@example.com"
        userSqlMapper.insert(UserRecord(null, email, "byemailuser", "\$2a\$10\$hash", true, Instant.now()))

        val found = userSqlMapper.selectByEmail(email)
        assertNotNull(found)
        assertEquals(email, found.email)
    }

    @Test
    fun `selectByEmail returns null for unknown email`() {
        assertNull(userSqlMapper.selectByEmail("nobody@example.com"))
    }

    @Test
    fun `existsByEmail returns true for existing email`() {
        val email = "exists@example.com"
        userSqlMapper.insert(UserRecord(null, email, "existsuser", "\$2a\$10\$hash", true, Instant.now()))

        assertTrue(userSqlMapper.existsByEmail(email))
    }

    @Test
    fun `existsByEmail returns false for unknown email`() {
        assertTrue(!userSqlMapper.existsByEmail("nobody2@example.com"))
    }

    @Test
    fun `existsByUsername returns true for existing username`() {
        userSqlMapper.insert(UserRecord(null, "uname@example.com", "uniqueuser", "\$2a\$10\$hash", true, Instant.now()))

        assertTrue(userSqlMapper.existsByUsername("uniqueuser"))
    }

    @Test
    fun `existsByUsername returns false for unknown username`() {
        assertTrue(!userSqlMapper.existsByUsername("nosuchuser"))
    }
}
