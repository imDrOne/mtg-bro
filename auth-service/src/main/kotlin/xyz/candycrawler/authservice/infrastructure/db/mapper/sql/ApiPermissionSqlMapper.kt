package xyz.candycrawler.authservice.infrastructure.db.mapper.sql

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import xyz.candycrawler.authservice.infrastructure.db.entity.ApiPermissionRecord

@Component
class ApiPermissionSqlMapper(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    internal fun selectByRoles(roles: List<String>): List<ApiPermissionRecord> {
        if (roles.isEmpty()) return emptyList()
        val sql = """
            SELECT DISTINCT p.id, p.name, p.description
            FROM api_permissions p
            JOIN role_api_permissions rap ON rap.permission_id = p.id
            WHERE rap.role IN (:roles)
            ORDER BY p.name
        """.trimIndent()
        return jdbc.query(sql, MapSqlParameterSource("roles", roles)) { rs, _ ->
            ApiPermissionRecord(
                id = rs.getLong("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
            )
        }
    }
}
