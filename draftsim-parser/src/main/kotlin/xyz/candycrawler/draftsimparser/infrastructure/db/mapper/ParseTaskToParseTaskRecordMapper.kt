package xyz.candycrawler.draftsimparser.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ParseTaskRecord
import java.util.function.Function

@Component
class ParseTaskToParseTaskRecordMapper : Function<ParseTask, ParseTaskRecord> {

    override fun apply(task: ParseTask): ParseTaskRecord = ParseTaskRecord(
        id = task.id,
        keyword = task.keyword,
        status = task.status.name,
        totalArticles = task.totalArticles,
        processedArticles = task.processedArticles,
        errorMessage = task.errorMessage,
        createdAt = task.createdAt,
        updatedAt = task.updatedAt,
    )
}
