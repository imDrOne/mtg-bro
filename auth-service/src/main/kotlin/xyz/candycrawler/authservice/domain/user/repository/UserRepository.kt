package xyz.candycrawler.authservice.domain.user.repository

import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.common.pagination.PageRequest
import xyz.candycrawler.common.pagination.PageResponse

interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun update(id: Long, block: (User) -> User): User
    fun findAll(filter: UserFilter, page: PageRequest): PageResponse<User>
}
