package xyz.candycrawler.draftsimparser.application.rest.dto.response

import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import java.time.LocalDateTime
import java.util.UUID

data class ParseTaskResponse(
    val id: UUID,
    val keyword: String,
    val status: String,
    val totalArticles: Int?,
    val processedArticles: Int,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun ParseTask.toResponse() = ParseTaskResponse(
    id = id!!,
    keyword = keyword,
    status = status.name,
    totalArticles = totalArticles,
    processedArticles = processedArticles,
    errorMessage = errorMessage,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
