package xyz.candycrawler.authservice.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.refreshtoken.model.RefreshToken
import xyz.candycrawler.authservice.domain.refreshtoken.repository.RefreshTokenRepository
import xyz.candycrawler.authservice.infrastructure.db.entity.RefreshTokenRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.RefreshTokenSqlMapper
import java.time.Instant

@Repository
@Transactional
class JdbcRefreshTokenRepository(
    private val sqlMapper: RefreshTokenSqlMapper,
) : RefreshTokenRepository {

    override fun save(token: RefreshToken): RefreshToken {
        val record = sqlMapper.insert(toRecord(token))
        return toDomain(record)
    }

    @Transactional(readOnly = true)
    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        sqlMapper.selectByTokenHash(tokenHash)?.let(::toDomain)

    override fun revoke(id: Long, replacedById: Long?, revokedAt: Instant) {
        sqlMapper.updateRevocation(id, replacedById, revokedAt)
    }

    override fun revokeAllForUser(userId: Long, revokedAt: Instant) {
        sqlMapper.revokeAllForUser(userId, revokedAt)
    }

    private fun toRecord(token: RefreshToken): RefreshTokenRecord =
        RefreshTokenRecord(
            id = token.id,
            userId = token.userId,
            tokenHash = token.tokenHash,
            issuedAt = token.issuedAt,
            expiresAt = token.expiresAt,
            revokedAt = token.revokedAt,
            replacedById = token.replacedById,
        )

    private fun toDomain(record: RefreshTokenRecord): RefreshToken =
        RefreshToken(
            id = record.id,
            userId = record.userId,
            tokenHash = record.tokenHash,
            issuedAt = record.issuedAt,
            expiresAt = record.expiresAt,
            revokedAt = record.revokedAt,
            replacedById = record.replacedById,
        )
}
