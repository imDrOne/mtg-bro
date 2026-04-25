package xyz.candycrawler.authservice.domain.refreshtoken.repository

import xyz.candycrawler.authservice.domain.refreshtoken.model.RefreshToken
import java.time.Instant

interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun revoke(id: Long, replacedById: Long?, revokedAt: Instant)
    fun revokeAllForUser(userId: Long, revokedAt: Instant)
}
