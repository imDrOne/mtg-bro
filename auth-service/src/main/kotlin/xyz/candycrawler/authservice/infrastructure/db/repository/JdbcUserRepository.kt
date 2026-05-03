package xyz.candycrawler.authservice.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.user.exception.UserNotFoundException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.infrastructure.db.mapper.UserRecordToUserMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.UserToUserRecordMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper
import xyz.candycrawler.common.pagination.PageRequest
import xyz.candycrawler.common.pagination.PageResponse
import kotlin.math.ceil

@Repository
@Transactional
class JdbcUserRepository(
    private val sqlMapper: UserSqlMapper,
    private val toRecord: UserToUserRecordMapper,
    private val toDomain: UserRecordToUserMapper,
) : UserRepository {

    override fun save(user: User): User = sqlMapper.insert(toRecord.map(user)).let(toDomain::map)

    @Transactional(readOnly = true)
    override fun findById(id: Long): User? = sqlMapper.selectById(id)?.let(toDomain::map)

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): User? = sqlMapper.selectByEmail(email)?.let(toDomain::map)

    @Transactional(readOnly = true)
    override fun existsByEmail(email: String): Boolean = sqlMapper.existsByEmail(email)

    @Transactional(readOnly = true)
    override fun existsByUsername(username: String): Boolean = sqlMapper.existsByUsername(username)

    override fun update(id: Long, block: (User) -> User): User {
        val existing = sqlMapper.selectById(id)?.let(toDomain::map)
            ?: throw UserNotFoundException(id)
        val updated = block(existing)
        return sqlMapper.update(toRecord.map(updated)).let(toDomain::map)
    }

    @Transactional(readOnly = true)
    override fun findAll(filter: UserFilter, page: PageRequest): PageResponse<User> {
        val total = sqlMapper.countAll(filter)
        val items = sqlMapper.findAll(filter, page).map(toDomain::map)
        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / page.size).toInt()
        return PageResponse(
            items = items,
            page = page.page,
            size = page.size,
            totalItems = total,
            totalPages = totalPages,
        )
    }
}
