package xyz.candycrawler.wizardstataggregator.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository.TrackedLimitedStatSetRepository
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.TrackedLimitedStatSetRecord
import xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper.TrackedLimitedStatSetSqlMapper
import java.time.LocalDate

@Repository
@Transactional
class ExposedTrackedLimitedStatSetRepository(private val sqlMapper: TrackedLimitedStatSetSqlMapper) :
    TrackedLimitedStatSetRepository {

    override fun save(trackedSet: TrackedLimitedStatSet): TrackedLimitedStatSet =
        sqlMapper.upsert(trackedSet.toRecord()).toDomain()

    override fun findAll(): List<TrackedLimitedStatSet> = sqlMapper.selectAll().map { it.toDomain() }

    override fun findActive(today: LocalDate): List<TrackedLimitedStatSet> =
        sqlMapper.selectActive(today).map { it.toDomain() }

    override fun deleteBySetCode(setCode: String) {
        sqlMapper.deleteBySetCode(setCode)
    }

    private fun TrackedLimitedStatSet.toRecord(): TrackedLimitedStatSetRecord = TrackedLimitedStatSetRecord(
        setCode = setCode,
        watchUntil = watchUntil,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TrackedLimitedStatSetRecord.toDomain(): TrackedLimitedStatSet = TrackedLimitedStatSet(
        setCode = setCode,
        watchUntil = watchUntil,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
