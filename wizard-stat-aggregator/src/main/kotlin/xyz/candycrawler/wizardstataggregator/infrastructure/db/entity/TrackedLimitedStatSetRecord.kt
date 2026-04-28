package xyz.candycrawler.wizardstataggregator.infrastructure.db.entity

import java.time.LocalDate
import java.time.LocalDateTime

data class TrackedLimitedStatSetRecord(
    val setCode: String,
    val watchUntil: LocalDate,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

