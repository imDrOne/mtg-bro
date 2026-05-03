package xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository

import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import java.time.LocalDate

interface TrackedLimitedStatSetRepository {
    fun save(trackedSet: TrackedLimitedStatSet): TrackedLimitedStatSet
    fun findAll(): List<TrackedLimitedStatSet>
    fun findActive(today: LocalDate): List<TrackedLimitedStatSet>
    fun deleteBySetCode(setCode: String)
}
