package xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.TrackedLimitedStatSetRecord
import xyz.candycrawler.wizardstataggregator.infrastructure.db.table.TrackedLimitedStatSetsTable
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class TrackedLimitedStatSetSqlMapper {

    internal fun upsert(record: TrackedLimitedStatSetRecord): TrackedLimitedStatSetRecord {
        val now = LocalDateTime.now()
        if (selectBySetCode(record.setCode) == null) {
            TrackedLimitedStatSetsTable.insert {
                it[setCode] = record.setCode
                it[watchUntil] = record.watchUntil
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            TrackedLimitedStatSetsTable.update({ TrackedLimitedStatSetsTable.setCode eq record.setCode }) {
                it[watchUntil] = record.watchUntil
                it[updatedAt] = now
            }
        }

        return requireNotNull(selectBySetCode(record.setCode))
    }

    internal fun selectAll(): List<TrackedLimitedStatSetRecord> = TrackedLimitedStatSetsTable.selectAll()
        .orderBy(TrackedLimitedStatSetsTable.setCode to SortOrder.ASC)
        .map { it.toRecord() }

    internal fun selectActive(today: LocalDate): List<TrackedLimitedStatSetRecord> =
        TrackedLimitedStatSetsTable.selectAll()
            .where { TrackedLimitedStatSetsTable.watchUntil greaterEq today }
            .orderBy(TrackedLimitedStatSetsTable.setCode to SortOrder.ASC)
            .map { it.toRecord() }

    internal fun deleteBySetCode(setCode: String) {
        TrackedLimitedStatSetsTable.deleteWhere { TrackedLimitedStatSetsTable.setCode eq setCode }
    }

    private fun selectBySetCode(setCode: String): TrackedLimitedStatSetRecord? = TrackedLimitedStatSetsTable.selectAll()
        .where { TrackedLimitedStatSetsTable.setCode eq setCode }
        .map { it.toRecord() }
        .singleOrNull()

    private fun ResultRow.toRecord(): TrackedLimitedStatSetRecord = TrackedLimitedStatSetRecord(
        setCode = this[TrackedLimitedStatSetsTable.setCode],
        watchUntil = this[TrackedLimitedStatSetsTable.watchUntil],
        createdAt = this[TrackedLimitedStatSetsTable.createdAt],
        updatedAt = this[TrackedLimitedStatSetsTable.updatedAt],
    )
}
