package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.infrastructure.db.entity.RefreshTokenRecord
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.RefreshTokenSqlMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshTokenSqlMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var refreshTokenSqlMapper: RefreshTokenSqlMapper

    @Autowired
    lateinit var userSqlMapper: UserSqlMapper

    private fun insertUser(): Long {
        val email = "rt-${UUID.randomUUID()}@example.com"
        val record = UserRecord(null, email, "u${UUID.randomUUID().toString().take(8)}", "\$2a\$10\$hash", true, Instant.now())
        return userSqlMapper.insert(record).id!!
    }

    @Test
    fun `insert assigns id and persists fields`() {
        val userId = insertUser()
        val now = Instant.now()
        val record = RefreshTokenRecord(
            id = null,
            userId = userId,
            tokenHash = "b".repeat(64),
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            revokedAt = null,
            replacedById = null,
        )

        val saved = refreshTokenSqlMapper.insert(record)

        assertNotNull(saved.id)
        assertTrue(saved.id!! > 0)
        assertEquals(userId, saved.userId)
        assertEquals("b".repeat(64), saved.tokenHash)
    }

    @Test
    fun `selectByTokenHash returns inserted record`() {
        val userId = insertUser()
        val hash = "c".repeat(64)
        val now = Instant.now()
        refreshTokenSqlMapper.insert(
            RefreshTokenRecord(null, userId, hash, now, now.plusSeconds(3600), null, null),
        )

        val found = refreshTokenSqlMapper.selectByTokenHash(hash)

        assertNotNull(found)
        assertEquals(userId, found.userId)
        assertEquals(hash, found.tokenHash)
        assertNull(found.revokedAt)
    }

    @Test
    fun `selectByTokenHash returns null for unknown hash`() {
        assertNull(refreshTokenSqlMapper.selectByTokenHash("d".repeat(64)))
    }

    @Test
    fun `updateRevocation marks token revoked and links replacement`() {
        val userId = insertUser()
        val now = Instant.now()
        val original = refreshTokenSqlMapper.insert(
            RefreshTokenRecord(null, userId, "e".repeat(64), now, now.plusSeconds(3600), null, null),
        )
        val replacement = refreshTokenSqlMapper.insert(
            RefreshTokenRecord(null, userId, "f".repeat(64), now, now.plusSeconds(3600), null, null),
        )

        refreshTokenSqlMapper.updateRevocation(original.id!!, replacement.id, now.plusSeconds(60))

        val reloaded = refreshTokenSqlMapper.selectByTokenHash("e".repeat(64))!!
        assertNotNull(reloaded.revokedAt)
        assertEquals(replacement.id, reloaded.replacedById)
    }

    @Test
    fun `revokeAllForUser revokes only non-revoked tokens for user`() {
        val userId = insertUser()
        val otherUserId = insertUser()
        val now = Instant.now()
        refreshTokenSqlMapper.insert(RefreshTokenRecord(null, userId, "g".repeat(64), now, now.plusSeconds(3600), null, null))
        refreshTokenSqlMapper.insert(RefreshTokenRecord(null, userId, "h".repeat(64), now, now.plusSeconds(3600), null, null))
        refreshTokenSqlMapper.insert(RefreshTokenRecord(null, otherUserId, "i".repeat(64), now, now.plusSeconds(3600), null, null))

        refreshTokenSqlMapper.revokeAllForUser(userId, now.plusSeconds(60))

        assertNotNull(refreshTokenSqlMapper.selectByTokenHash("g".repeat(64))!!.revokedAt)
        assertNotNull(refreshTokenSqlMapper.selectByTokenHash("h".repeat(64))!!.revokedAt)
        assertNull(refreshTokenSqlMapper.selectByTokenHash("i".repeat(64))!!.revokedAt)
    }
}
