package xyz.candycrawler.authservice.infrastructure.db.mapper.sql

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class UserRoleSqlMapper(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    internal fun insert(userId: Long, role: String) {
        val sql = """
            INSERT INTO user_roles (user_id, role)
            VALUES (:userId, :role)
            ON CONFLICT DO NOTHING
        """.trimIndent()

        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("role", role),
        )
    }

    internal fun selectByUserId(userId: Long): List<String> {
        val sql = "SELECT role FROM user_roles WHERE user_id = :userId ORDER BY granted_at"
        return jdbc.queryForList(sql, MapSqlParameterSource("userId", userId), String::class.java)
    }
}
