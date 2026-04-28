package xyz.candycrawler.wizardstataggregator.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository.TrackedLimitedStatSetRepository
import java.time.LocalDate

@Service
class TrackedLimitedStatSetService(
    private val repository: TrackedLimitedStatSetRepository,
) {

    fun upsert(setCode: String, watchUntil: LocalDate): TrackedLimitedStatSet =
        repository.save(TrackedLimitedStatSet(normalizeSetCode(setCode), watchUntil))

    fun findAll(): List<TrackedLimitedStatSet> = repository.findAll()

    fun findActive(today: LocalDate = LocalDate.now()): List<TrackedLimitedStatSet> =
        repository.findActive(today)

    fun delete(setCode: String) {
        repository.deleteBySetCode(normalizeSetCode(setCode))
    }

    private fun normalizeSetCode(setCode: String): String = setCode.trim().uppercase()
}
