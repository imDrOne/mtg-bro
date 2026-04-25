package xyz.candycrawler.authservice.domain.permission.repository

import xyz.candycrawler.authservice.domain.permission.model.ApiPermission
import xyz.candycrawler.authservice.domain.user.model.UserRole

interface ApiPermissionRepository {
    fun findByRole(role: UserRole): List<ApiPermission>
    fun findByRoles(roles: List<UserRole>): List<ApiPermission>
}
