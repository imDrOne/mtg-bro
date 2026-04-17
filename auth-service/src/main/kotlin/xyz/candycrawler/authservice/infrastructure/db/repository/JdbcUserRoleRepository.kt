package xyz.candycrawler.authservice.infrastructure.db.repository

import org.springframework.stereotype.Service
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserRoleSqlMapper

@Service
class JdbcUserRoleRepository(
    private val userRoleSqlMapper: UserRoleSqlMapper,
) : UserRoleRepository {

    override fun assignRole(userId: Long, role: UserRole) {
        userRoleSqlMapper.insert(userId, role.name)
    }

    override fun findByUserId(userId: Long): List<UserRole> =
        userRoleSqlMapper.selectByUserId(userId).map { UserRole.valueOf(it) }
}
