package xyz.candycrawler.authservice.infrastructure.db.repository

import org.springframework.stereotype.Repository
import xyz.candycrawler.authservice.domain.permission.model.ApiPermission
import xyz.candycrawler.authservice.domain.permission.repository.ApiPermissionRepository
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.ApiPermissionSqlMapper

@Repository
class JdbcApiPermissionRepository(private val sqlMapper: ApiPermissionSqlMapper) : ApiPermissionRepository {

    override fun findByRole(role: UserRole): List<ApiPermission> = findByRoles(listOf(role))

    override fun findByRoles(roles: List<UserRole>): List<ApiPermission> =
        sqlMapper.selectByRoles(roles.map { it.name })
            .map { record -> ApiPermission(id = record.id, name = record.name, description = record.description) }
}
