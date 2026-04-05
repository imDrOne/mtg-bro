package xyz.candycrawler.collectionmanager.application.rest.handler

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import xyz.candycrawler.collectionmanager.domain.card.exception.CardNotFoundException
import xyz.candycrawler.collectionmanager.domain.card.exception.InvalidCardException
import xyz.candycrawler.collectionmanager.domain.collection.exception.InvalidCollectionEntryException
import xyz.candycrawler.collectionmanager.domain.deck.exception.DeckNotFoundException
import xyz.candycrawler.collectionmanager.domain.deck.exception.InvalidDeckException
import xyz.candycrawler.collectionmanager.domain.tribal.exception.InvalidTribalQueryException
import xyz.candycrawler.collectionmanager.infrastructure.client.ngrok.NgrokUnavailableException

@RestControllerAdvice
class DomainExceptionHandler {

    @ExceptionHandler(CardNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleCardNotFound(ex: CardNotFoundException): ErrorResponse = ErrorResponse(
        status = HttpStatus.NOT_FOUND.value(),
        error = HttpStatus.NOT_FOUND.reasonPhrase,
        message = ex.message ?: "Card not found",
    )

    @ExceptionHandler(InvalidCardException::class, InvalidCollectionEntryException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleDomainValidation(ex: RuntimeException): ErrorResponse = ErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
        error = HttpStatus.UNPROCESSABLE_ENTITY.reasonPhrase,
        message = ex.message ?: "Domain validation failed",
    )

    @ExceptionHandler(DeckNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleDeckNotFound(ex: DeckNotFoundException): ErrorResponse = ErrorResponse(
        status = HttpStatus.NOT_FOUND.value(),
        error = HttpStatus.NOT_FOUND.reasonPhrase,
        message = ex.message ?: "Deck not found",
    )

    @ExceptionHandler(InvalidDeckException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleDeckValidation(ex: InvalidDeckException): ErrorResponse = ErrorResponse(
        status = HttpStatus. UNPROCESSABLE_CONTENT.value(),
        error = HttpStatus.UNPROCESSABLE_CONTENT.reasonPhrase,
        message = ex.message ?: "Deck validation failed",
    )

    @ExceptionHandler(InvalidTribalQueryException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidTribalQuery(ex: InvalidTribalQueryException): ErrorResponse = ErrorResponse(
        status = HttpStatus.BAD_REQUEST.value(),
        error = HttpStatus.BAD_REQUEST.reasonPhrase,
        message = ex.message ?: "Invalid tribal query",
    )

    @ExceptionHandler(NgrokUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleNgrokUnavailable(ex: NgrokUnavailableException): ErrorResponse = ErrorResponse(
        status = HttpStatus.SERVICE_UNAVAILABLE.value(),
        error = HttpStatus.SERVICE_UNAVAILABLE.reasonPhrase,
        message = ex.message ?: "ngrok is unavailable",
    )
}
