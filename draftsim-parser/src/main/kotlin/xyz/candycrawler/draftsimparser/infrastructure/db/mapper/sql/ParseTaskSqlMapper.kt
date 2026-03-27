package xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ParseTaskRecord
import xyz.candycrawler.draftsimparser.infrastructure.db.table.ParseTasksTable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Component
class ParseTaskSqlMapper {

    internal fun insert(record: ParseTaskRecord): ParseTaskRecord {
        val newId = Uuid.random()
        ParseTasksTable.insert { row ->
            row[id] = newId
            row[keyword] = record.keyword
            row[status] = record.status
            row[totalArticles] = record.totalArticles
            row[processedArticles] = record.processedArticles
            row[errorMessage] = record.errorMessage
            row[createdAt] = record.createdAt
            row[updatedAt] = record.updatedAt
        }
        return record.copy(id = UUID.fromString(newId.toString()))
    }

    internal fun selectById(id: UUID): ParseTaskRecord? =
        ParseTasksTable.selectAll()
            .where { ParseTasksTable.id eq Uuid.parse(id.toString()) }
            .map { it.toRecord() }
            .singleOrNull()

    internal fun update(record: ParseTaskRecord) {
        ParseTasksTable.update(where = { ParseTasksTable.id eq Uuid.parse(record.id.toString()) }) { row ->
            row[keyword] = record.keyword
            row[status] = record.status
            row[totalArticles] = record.totalArticles
            row[processedArticles] = record.processedArticles
            row[errorMessage] = record.errorMessage
            row[updatedAt] = record.updatedAt
        }
    }

    internal fun incrementProcessedArticles(id: UUID, delta: Int) {
        ParseTasksTable.update(where = { ParseTasksTable.id eq Uuid.parse(id.toString()) }) { row ->
            row[processedArticles] = ParseTasksTable.processedArticles + delta
        }
    }

    private fun ResultRow.toRecord(): ParseTaskRecord = ParseTaskRecord(
        id = UUID.fromString(this[ParseTasksTable.id].toString()),
        keyword = this[ParseTasksTable.keyword],
        status = this[ParseTasksTable.status],
        totalArticles = this[ParseTasksTable.totalArticles],
        processedArticles = this[ParseTasksTable.processedArticles],
        errorMessage = this[ParseTasksTable.errorMessage],
        createdAt = this[ParseTasksTable.createdAt],
        updatedAt = this[ParseTasksTable.updatedAt],
    )
}
