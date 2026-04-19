package xyz.candycrawler.authservice.application.rest.handler

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.exception.UserNotFoundException

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
)

@RestControllerAdvice
class DomainExceptionHandler {

    private val log = LoggerFactory.getLogger(DomainExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotFound(ex: UserNotFoundException): ErrorResponse {
        log.warn("UserNotFoundException: {}", ex.message)
        return ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = "Something went wrong, please try again",
        )
    }

    @ExceptionHandler(UserInvalidException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleUserInvalid(ex: UserInvalidException): ErrorResponse {
        log.warn("UserInvalidException: {}", ex.message)
        return ErrorResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
            error = HttpStatus.UNPROCESSABLE_ENTITY.reasonPhrase,
            message = "Something went wrong, please try again",
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidClientMetadata(ex: IllegalArgumentException): Map<String, String> {
        log.warn("IllegalArgumentException: {}", ex.message)
        return mapOf(
            "error" to "invalid_client_metadata",
            "error_description" to (ex.message ?: "Invalid client metadata"),
        )
    }
}
