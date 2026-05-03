package xyz.candycrawler.draftsimparser.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.draftsimparser.domain.parsetask.exception.ParseTaskNotFoundException
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.ParseTaskRecordToParseTaskMapper
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.ParseTaskToParseTaskRecordMapper
import xyz.candycrawler.draftsimparser.infrastructure.db.mapper.sql.ParseTaskSqlMapper
import java.util.UUID

@Repository
@Transactional
class ExposedParseTaskRepository(
    private val sqlMapper: ParseTaskSqlMapper,
    private val toDomain: ParseTaskRecordToParseTaskMapper,
    private val toRecord: ParseTaskToParseTaskRecordMapper,
) : ParseTaskRepository {

    override fun save(task: ParseTask): ParseTask = sqlMapper.insert(toRecord.apply(task)).let(toDomain::apply)

    @Transactional(readOnly = true)
    override fun findById(id: UUID): ParseTask = sqlMapper.selectById(id)?.let(toDomain::apply)
        ?: throw ParseTaskNotFoundException(id)

    override fun update(id: UUID, block: (ParseTask) -> ParseTask): ParseTask {
        val existing = findById(id)
        val updated = block(existing)
        sqlMapper.update(toRecord.apply(updated))
        return updated
    }

    override fun incrementProcessedArticles(id: UUID, delta: Int) = sqlMapper.incrementProcessedArticles(id, delta)
}
