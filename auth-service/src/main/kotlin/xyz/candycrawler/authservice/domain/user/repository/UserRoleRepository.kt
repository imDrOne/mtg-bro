package xyz.candycrawler.authservice.domain.user.repository

import xyz.candycrawler.authservice.domain.user.model.UserRole

interface UserRoleRepository {
    fun assignRole(userId: Long, role: UserRole)
    fun findByUserId(userId: Long): List<UserRole>
}
