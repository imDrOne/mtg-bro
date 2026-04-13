package xyz.candycrawler.authservice.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.infrastructure.db.mapper.UserRecordToUserMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.UserToUserRecordMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper

@Repository
@Transactional
class JdbcUserRepository(
    private val sqlMapper: UserSqlMapper,
    private val toRecord: UserToUserRecordMapper,
    private val toDomain: UserRecordToUserMapper,
) : UserRepository {

    override fun save(user: User): User =
        sqlMapper.insert(toRecord.map(user)).let(toDomain::map)

    @Transactional(readOnly = true)
    override fun findById(id: Long): User? =
        sqlMapper.selectById(id)?.let(toDomain::map)

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): User? =
        sqlMapper.selectByEmail(email)?.let(toDomain::map)

    @Transactional(readOnly = true)
    override fun existsByEmail(email: String): Boolean =
        sqlMapper.existsByEmail(email)

    @Transactional(readOnly = true)
    override fun existsByUsername(username: String): Boolean =
        sqlMapper.existsByUsername(username)
}
