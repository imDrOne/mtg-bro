package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserRoleSqlMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRoleSqlMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var userRoleSqlMapper: UserRoleSqlMapper

    @Autowired
    lateinit var userSqlMapper: UserSqlMapper

    private fun insertUser(email: String, username: String): Long {
        val record = UserRecord(null, email, username, "\$2a\$10\$hash", true, Instant.now())
        return userSqlMapper.insert(record).id!!
    }

    @Test
    fun `insert assigns a role to a user`() {
        val userId = insertUser("role1@example.com", "roleuser1")
        userRoleSqlMapper.insert(userId, "FREE")
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertEquals(listOf("FREE"), roles)
    }

    @Test
    fun `insert multiple roles to same user`() {
        val userId = insertUser("role2@example.com", "roleuser2")
        userRoleSqlMapper.insert(userId, "FREE")
        userRoleSqlMapper.insert(userId, "PRO")
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertEquals(2, roles.size)
        assertTrue(roles.containsAll(listOf("FREE", "PRO")))
    }

    @Test
    fun `insert is idempotent - duplicate role is ignored`() {
        val userId = insertUser("role3@example.com", "roleuser3")
        userRoleSqlMapper.insert(userId, "FREE")
        userRoleSqlMapper.insert(userId, "FREE") // should not throw
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertEquals(listOf("FREE"), roles)
    }

    @Test
    fun `selectByUserId returns empty list for user with no roles`() {
        val userId = insertUser("role4@example.com", "roleuser4")
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertTrue(roles.isEmpty())
    }
}
