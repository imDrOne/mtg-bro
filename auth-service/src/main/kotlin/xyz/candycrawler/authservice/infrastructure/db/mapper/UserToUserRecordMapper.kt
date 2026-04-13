package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord

@Component
class UserToUserRecordMapper {
    fun map(user: User): UserRecord = UserRecord(
        id = user.id,
        email = user.email,
        username = user.username,
        passwordHash = user.passwordHash,
        enabled = user.enabled,
        createdAt = user.createdAt,
    )
}
