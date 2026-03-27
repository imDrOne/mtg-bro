package xyz.candycrawler.draftsimparser.domain.parsetask.model

import xyz.candycrawler.draftsimparser.domain.parsetask.exception.ParseTaskInvalidException
import java.time.LocalDateTime
import java.util.UUID

data class ParseTask(
    val id: UUID?,
    val keyword: String,
    val status: ParseTaskStatus,
    val totalArticles: Int?,
    val processedArticles: Int,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        fun invalid(reason: String): Nothing = throw ParseTaskInvalidException(reason)

        if (keyword.isBlank()) invalid("keyword must not be blank")
        if (processedArticles < 0) invalid("processedArticles must be non-negative")
        totalArticles?.let { if (it < 0) invalid("totalArticles must be non-negative") }
    }
}
