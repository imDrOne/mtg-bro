package xyz.candycrawler.draftsimparser.application.service

import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import xyz.candycrawler.draftsimparser.application.port.AlertPublisher
import xyz.candycrawler.draftsimparser.configuration.TelegramAlertProperties
import kotlin.test.Test

class ParseAlertServiceTest {

    private val alertPublisher = mock<AlertPublisher>()

    @Test
    fun `articleAnalysisSucceeded sends alert when perArticleSuccess is true`() {
        val service = ParseAlertService(alertPublisher, TelegramAlertProperties(perArticleSuccess = true))

        service.articleAnalysisSucceeded(
            articleId = 42,
            slug = "test-slug",
            insightCount = 5,
            articleType = "pick_order",
        )

        verify(alertPublisher).send(
            org.mockito.kotlin.argThat { message ->
                message.contains("42") &&
                    message.contains("test-slug") &&
                    message.contains("pick_order") &&
                    message.contains("5")
            },
        )
    }

    @Test
    fun `articleAnalysisSucceeded does not send alert when perArticleSuccess is false`() {
        val service = ParseAlertService(alertPublisher, TelegramAlertProperties(perArticleSuccess = false))

        service.articleAnalysisSucceeded(
            articleId = 42,
            slug = "test-slug",
            insightCount = 5,
            articleType = "pick_order",
        )

        verify(alertPublisher, never()).send(org.mockito.kotlin.any())
    }

    @Test
    fun `articleAnalysisFailed sends alert when perArticleFailure is true`() {
        val service = ParseAlertService(alertPublisher, TelegramAlertProperties(perArticleFailure = true))
        val error = RuntimeException("LLM timeout")

        service.articleAnalysisFailed(articleId = 7, slug = "failed-slug", error = error)

        verify(alertPublisher).send(
            org.mockito.kotlin.argThat { message ->
                message.contains("7") &&
                    message.contains("failed-slug") &&
                    message.contains("LLM timeout")
            },
        )
    }

    @Test
    fun `articleAnalysisFailed does not send alert when perArticleFailure is false`() {
        val service = ParseAlertService(alertPublisher, TelegramAlertProperties(perArticleFailure = false))

        service.articleAnalysisFailed(articleId = 7, slug = "failed-slug", error = RuntimeException("LLM timeout"))

        verify(alertPublisher, never()).send(org.mockito.kotlin.any())
    }
}
