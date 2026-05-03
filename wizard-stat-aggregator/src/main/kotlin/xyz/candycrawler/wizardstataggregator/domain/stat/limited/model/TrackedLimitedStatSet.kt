package xyz.candycrawler.wizardstataggregator.domain.stat.limited.model

import xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception.InvalidTrackedLimitedStatSetException
import java.time.LocalDate
import java.time.LocalDateTime

data class TrackedLimitedStatSet(
    val setCode: String,
    val watchUntil: LocalDate,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    init {
        fun invalid(reason: String): Nothing = throw InvalidTrackedLimitedStatSetException(reason)

        if (setCode.isBlank()) invalid("setCode must not be blank")
        if (setCode.length > MAX_SET_CODE_LENGTH) {
            invalid("setCode must not be longer than $MAX_SET_CODE_LENGTH characters")
        }
    }

    fun isActive(today: LocalDate): Boolean = !watchUntil.isBefore(today)

    private companion object {
        const val MAX_SET_CODE_LENGTH = 10
    }
}
