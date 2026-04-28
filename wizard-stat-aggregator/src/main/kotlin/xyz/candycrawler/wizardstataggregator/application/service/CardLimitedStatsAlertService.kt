package xyz.candycrawler.wizardstataggregator.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.wizardstataggregator.infrastructure.client.telegram.TelegramAlertClient

@Service
class CardLimitedStatsAlertService(
    private val telegramAlertClient: TelegramAlertClient,
) {

    fun collectionStarted(setCode: String) {
        telegramAlertClient.send(
            """
            🚀 Wizard stats parsing started
            set: $setCode
            #wizard_stats #limited_stats #started
            """.trimIndent()
        )
    }

    fun parsingFailed(setCode: String, matchType: String, error: Throwable) {
        telegramAlertClient.send(
            """
            ⚠️ Wizard stats parsing error
            set: $setCode
            matchType: $matchType
            error: ${error.shortMessage()}
            #wizard_stats #limited_stats #parse_error
            """.trimIndent()
        )
    }

    fun savingFailed(setCode: String, matchType: String, recordCount: Int, error: Throwable) {
        telegramAlertClient.send(
            """
            ❌ Wizard stats DB save error
            set: $setCode
            matchType: $matchType
            records: $recordCount
            error: ${error.shortMessage()}
            #wizard_stats #limited_stats #db_error
            """.trimIndent()
        )
    }

    fun collectionFinished(setCode: String, successfulMatchTypes: Int, totalMatchTypes: Int) {
        telegramAlertClient.send(
            """
            ✅ Wizard stats parsing finished
            set: $setCode
            successful: $successfulMatchTypes/$totalMatchTypes
            #wizard_stats #limited_stats #finished
            """.trimIndent()
        )
    }

    private fun Throwable.shortMessage(): String =
        (message ?: javaClass.simpleName).take(MAX_ERROR_LENGTH)

    companion object {
        private const val MAX_ERROR_LENGTH = 500
    }
}
