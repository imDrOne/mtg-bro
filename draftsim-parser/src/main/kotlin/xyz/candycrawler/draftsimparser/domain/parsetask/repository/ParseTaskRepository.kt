package xyz.candycrawler.draftsimparser.domain.parsetask.repository

import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import java.util.UUID

interface ParseTaskRepository {
    fun save(task: ParseTask): ParseTask
    fun findById(id: UUID): ParseTask
    fun update(id: UUID, block: (ParseTask) -> ParseTask): ParseTask
    fun incrementProcessedArticles(id: UUID, delta: Int)
}
