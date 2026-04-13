package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord

@Component
class UserRecordToUserMapper {
    fun map(record: UserRecord): User = User(
        id = record.id,
        email = record.email,
        username = record.username,
        passwordHash = record.passwordHash,
        enabled = record.enabled,
        createdAt = record.createdAt,
    )
}
