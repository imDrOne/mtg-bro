package xyz.candycrawler.draftsimparser.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTaskStatus
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ParseTaskRecord
import java.util.function.Function

@Component
class ParseTaskRecordToParseTaskMapper : Function<ParseTaskRecord, ParseTask> {

    override fun apply(record: ParseTaskRecord): ParseTask = ParseTask(
        id = record.id,
        keyword = record.keyword,
        status = ParseTaskStatus.valueOf(record.status),
        totalArticles = record.totalArticles,
        processedArticles = record.processedArticles,
        errorMessage = record.errorMessage,
        createdAt = record.createdAt,
        updatedAt = record.updatedAt,
    )
}
