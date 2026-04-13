package xyz.candycrawler.authservice.infrastructure.db.mapper.sql

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord
import java.sql.ResultSet
import java.sql.Timestamp

@Component
class UserSqlMapper(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    internal fun insert(record: UserRecord): UserRecord {
        val sql = """
            INSERT INTO users (email, username, password_hash, enabled, created_at)
            VALUES (:email, :username, :passwordHash, :enabled, :createdAt)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("email", record.email)
            .addValue("username", record.username)
            .addValue("passwordHash", record.passwordHash)
            .addValue("enabled", record.enabled)
            .addValue("createdAt", Timestamp.from(record.createdAt))

        val keyHolder = GeneratedKeyHolder()
        jdbc.update(sql, params, keyHolder, arrayOf("id", "created_at"))

        val keys = keyHolder.keys!!
        val generatedId = (keys["id"] as Number).toLong()
        val generatedAt = (keys["created_at"] as Timestamp).toInstant()

        return record.copy(id = generatedId, createdAt = generatedAt)
    }

    internal fun selectById(id: Long): UserRecord? {
        val sql = "SELECT id, email, username, password_hash, enabled, created_at FROM users WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id), ::mapRow).singleOrNull()
    }

    internal fun selectByEmail(email: String): UserRecord? {
        val sql = "SELECT id, email, username, password_hash, enabled, created_at FROM users WHERE email = :email"
        return jdbc.query(sql, MapSqlParameterSource("email", email), ::mapRow).singleOrNull()
    }

    internal fun existsByEmail(email: String): Boolean {
        val sql = "SELECT COUNT(1) FROM users WHERE email = :email"
        return (jdbc.queryForObject(sql, MapSqlParameterSource("email", email), Long::class.java) ?: 0L) > 0
    }

    internal fun existsByUsername(username: String): Boolean {
        val sql = "SELECT COUNT(1) FROM users WHERE username = :username"
        return (jdbc.queryForObject(sql, MapSqlParameterSource("username", username), Long::class.java) ?: 0L) > 0
    }

    @Suppress("UNUSED_PARAMETER")
    private fun mapRow(rs: ResultSet, rowNum: Int): UserRecord = UserRecord(
        id = rs.getLong("id"),
        email = rs.getString("email"),
        username = rs.getString("username"),
        passwordHash = rs.getString("password_hash"),
        enabled = rs.getBoolean("enabled"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
