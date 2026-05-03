package xyz.candycrawler.authservice.infrastructure.db.mapper.sql

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import xyz.candycrawler.authservice.infrastructure.db.entity.RefreshTokenRecord
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Component
class RefreshTokenSqlMapper(private val jdbc: NamedParameterJdbcTemplate) {

    internal fun insert(record: RefreshTokenRecord): RefreshTokenRecord {
        val sql = """
            INSERT INTO refresh_tokens (user_id, token_hash, issued_at, expires_at, revoked_at, replaced_by_id)
            VALUES (:userId, :tokenHash, :issuedAt, :expiresAt, :revokedAt, :replacedById)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", record.userId)
            .addValue("tokenHash", record.tokenHash)
            .addValue("issuedAt", Timestamp.from(record.issuedAt))
            .addValue("expiresAt", Timestamp.from(record.expiresAt))
            .addValue("revokedAt", record.revokedAt?.let { Timestamp.from(it) })
            .addValue("replacedById", record.replacedById)

        val keyHolder = GeneratedKeyHolder()
        jdbc.update(sql, params, keyHolder, arrayOf("id"))
        val id = (keyHolder.keys?.get("id") as Number).toLong()
        return record.copy(id = id)
    }

    internal fun selectByTokenHash(tokenHash: String): RefreshTokenRecord? {
        val sql = "SELECT * FROM refresh_tokens WHERE token_hash = :tokenHash"
        return jdbc.query(sql, MapSqlParameterSource("tokenHash", tokenHash), ::map).firstOrNull()
    }

    internal fun updateRevocation(id: Long, replacedById: Long?, revokedAt: Instant) {
        val sql = """
            UPDATE refresh_tokens
            SET revoked_at = :revokedAt, replaced_by_id = :replacedById
            WHERE id = :id
        """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id)
                .addValue("replacedById", replacedById)
                .addValue("revokedAt", Timestamp.from(revokedAt)),
        )
    }

    internal fun revokeAllForUser(userId: Long, revokedAt: Instant) {
        val sql = """
            UPDATE refresh_tokens
            SET revoked_at = :revokedAt
            WHERE user_id = :userId AND revoked_at IS NULL
        """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("revokedAt", Timestamp.from(revokedAt)),
        )
    }

    private fun map(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int): RefreshTokenRecord = RefreshTokenRecord(
        id = rs.getLong("id"),
        userId = rs.getLong("user_id"),
        tokenHash = rs.getString("token_hash"),
        issuedAt = rs.getTimestamp("issued_at").toInstant(),
        expiresAt = rs.getTimestamp("expires_at").toInstant(),
        revokedAt = rs.getTimestamp("revoked_at")?.toInstant(),
        replacedById = rs.getObject("replaced_by_id") as Long?,
    )
}
