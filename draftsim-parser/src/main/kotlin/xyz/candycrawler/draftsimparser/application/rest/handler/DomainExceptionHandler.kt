package xyz.candycrawler.draftsimparser.application.rest.handler

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import xyz.candycrawler.draftsimparser.domain.article.exception.ArticleInvalidException
import xyz.candycrawler.draftsimparser.domain.article.exception.ArticleNotFoundException
import xyz.candycrawler.draftsimparser.domain.parsetask.exception.ParseTaskInvalidException
import xyz.candycrawler.draftsimparser.domain.parsetask.exception.ParseTaskNotFoundException

@RestControllerAdvice
class DomainExceptionHandler {

    @ExceptionHandler(ParseTaskNotFoundException::class, ArticleNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): ErrorResponse = ErrorResponse(
        status = HttpStatus.NOT_FOUND.value(),
        error = HttpStatus.NOT_FOUND.reasonPhrase,
        message = ex.message ?: "Not found",
    )

    @ExceptionHandler(ParseTaskInvalidException::class, ArticleInvalidException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleDomainValidation(ex: RuntimeException): ErrorResponse = ErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
        error = HttpStatus.UNPROCESSABLE_ENTITY.reasonPhrase,
        message = ex.message ?: "Domain validation failed",
    )
}
