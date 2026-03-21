package xyz.candycrawler.wizardstataggregator.application.rest

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception.CardLimitedStatsNotFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CardLimitedStatsNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: CardLimitedStatsNotFoundException): Map<String, String> =
        mapOf("error" to (ex.message ?: "Not found"))
}
