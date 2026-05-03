package xyz.candycrawler.wizardstataggregator.infrastructure.client.telegram

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TelegramAlertClient(
    @Qualifier("telegramRestClient") private val telegramRestClient: RestClient,
    @Value("\${infrastructure.alerts.telegram.enabled:false}") private val enabled: Boolean,
    @Value("\${infrastructure.alerts.telegram.bot-token:}") private val botToken: String,
    @Value("\${infrastructure.alerts.telegram.chat-id:}") private val chatId: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun send(message: String) {
        if (!enabled || botToken.isBlank() || chatId.isBlank()) {
            log.debug(
                "Telegram alert skipped: enabled={}, tokenSet={}, chatIdSet={}",
                enabled,
                botToken.isNotBlank(),
                chatId.isNotBlank(),
            )
            return
        }

        runCatching {
            telegramRestClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .body(
                    mapOf(
                        "chat_id" to chatId,
                        "text" to message,
                        "disable_web_page_preview" to true,
                    ),
                )
                .retrieve()
                .toBodilessEntity()
        }.onFailure {
            log.warn("Telegram alert delivery failed", it)
        }
    }
}
