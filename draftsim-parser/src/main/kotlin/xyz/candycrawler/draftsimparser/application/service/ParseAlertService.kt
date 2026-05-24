package xyz.candycrawler.draftsimparser.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.AlertPublisher
import xyz.candycrawler.draftsimparser.configuration.TelegramAlertProperties
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class ParseAlertService(
    private val alertPublisher: AlertPublisher,
    private val telegramAlertProperties: TelegramAlertProperties,
) {

    fun parsingStarted(taskId: UUID, keyword: String) {
        alertPublisher.send(
            """
            🚀 Draftsim parsing started
            taskId: $taskId
            keyword: $keyword
            #draftsim #parse #started
            """.trimIndent(),
        )
    }

    fun schedulerRunStarted(setCodes: List<String>) {
        alertPublisher.send(
            """
            🛰️ Draftsim scheduled parse started
            sets: ${setCodes.joinToString()}
            #draftsim #parse #scheduler #started
            """.trimIndent(),
        )
    }

    fun parsingFinished(
        taskId: UUID,
        keyword: String,
        totalArticles: Int,
        savedArticles: Int,
        queuedForAnalysis: Int,
    ) {
        alertPublisher.send(
            """
            ✅ Draftsim parsing finished
            taskId: $taskId
            keyword: $keyword
            total: $totalArticles | saved: $savedArticles | queued: $queuedForAnalysis
            #draftsim #parse #finished
            """.trimIndent(),
        )
    }

    fun articleParsingFailed(taskId: UUID, keyword: String, postId: Long?, postUrl: String?, error: Throwable) {
        alertPublisher.send(
            """
            ⚠️ Draftsim article parse error
            taskId: $taskId
            keyword: $keyword
            postId: ${postId ?: "unknown"}
            url: ${postUrl ?: "unknown"}
            error: ${error.shortMessage()}
            #draftsim #parse #article_error
            """.trimIndent(),
        )
    }

    fun parseTaskFailed(taskId: UUID, keyword: String, error: Throwable) {
        alertPublisher.send(
            """
            ❌ Draftsim parse task failed
            taskId: $taskId
            keyword: $keyword
            error: ${error.shortMessage()}
            #draftsim #parse #failed
            """.trimIndent(),
        )
    }

    fun articleAnalysisSucceeded(articleId: Long, slug: String, insightCount: Int, articleType: String) {
        if (!telegramAlertProperties.perArticleSuccess) return
        alertPublisher.send(
            """
            ✅ Article analysis done
            id: $articleId | slug: $slug
            type: $articleType | insights: $insightCount
            #draftsim #analysis #success
            """.trimIndent(),
        )
    }

    fun articleAnalysisFailed(articleId: Long, slug: String, error: Throwable) {
        if (!telegramAlertProperties.perArticleFailure) return
        alertPublisher.send(
            """
            ❌ Article analysis failed
            id: $articleId | slug: $slug
            error: ${error.shortMessage()}
            #draftsim #analysis #failed
            """.trimIndent(),
        )
    }

    fun schedulerSkippedDueToCooldown(lastManual: Instant, cooldown: Duration) {
        alertPublisher.send(
            """
            ⏸️ Scheduled parse skipped (manual cooldown)
            last manual: $lastManual
            cooldown: $cooldown
            #draftsim #parse #scheduler #skipped
            """.trimIndent(),
        )
    }

    private fun Throwable.shortMessage(): String = (message ?: javaClass.simpleName).take(MAX_ERROR_LENGTH)

    companion object {
        private const val MAX_ERROR_LENGTH = 500
    }
}
