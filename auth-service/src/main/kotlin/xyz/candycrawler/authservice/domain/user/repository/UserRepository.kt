package xyz.candycrawler.authservice.domain.user.repository

import xyz.candycrawler.authservice.domain.user.model.User

interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
}
