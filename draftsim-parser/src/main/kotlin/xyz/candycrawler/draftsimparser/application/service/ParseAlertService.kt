package xyz.candycrawler.draftsimparser.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.infrastructure.client.telegram.TelegramAlertClient
import java.util.UUID

@Service
class ParseAlertService(
    private val telegramAlertClient: TelegramAlertClient,
) {

    fun parsingStarted(taskId: UUID, keyword: String) {
        telegramAlertClient.send(
            """
            🚀 Draftsim parsing started
            taskId: $taskId
            keyword: $keyword
            #draftsim #parse #started
            """.trimIndent()
        )
    }

    fun articleParsingFailed(taskId: UUID, keyword: String, postId: Long?, postUrl: String?, error: Throwable) {
        telegramAlertClient.send(
            """
            ⚠️ Draftsim article parse error
            taskId: $taskId
            keyword: $keyword
            postId: ${postId ?: "unknown"}
            url: ${postUrl ?: "unknown"}
            error: ${error.shortMessage()}
            #draftsim #parse #article_error
            """.trimIndent()
        )
    }

    fun parseTaskFailed(taskId: UUID, keyword: String, error: Throwable) {
        telegramAlertClient.send(
            """
            ❌ Draftsim parse task failed
            taskId: $taskId
            keyword: $keyword
            error: ${error.shortMessage()}
            #draftsim #parse #failed
            """.trimIndent()
        )
    }

    private fun Throwable.shortMessage(): String =
        (message ?: javaClass.simpleName).take(MAX_ERROR_LENGTH)

    companion object {
        private const val MAX_ERROR_LENGTH = 500
    }
}
