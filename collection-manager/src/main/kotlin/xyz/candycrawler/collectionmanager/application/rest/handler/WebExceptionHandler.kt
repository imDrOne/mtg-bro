package xyz.candycrawler.collectionmanager.application.rest.handler

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class WebExceptionHandler {

    @ExceptionHandler(RestClientResponseException::class)
    fun handleRestClientError(ex: RestClientResponseException): ResponseEntity<String> = ResponseEntity
        .status(ex.statusCode)
        .contentType(MediaType.APPLICATION_JSON)
        .body(ex.responseBodyAsString)

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ErrorResponse = ErrorResponse(
        status = HttpStatus.BAD_REQUEST.value(),
        error = HttpStatus.BAD_REQUEST.reasonPhrase,
        message = "Required parameter '${ex.parameterName}' of type '${ex.parameterType}' is missing",
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): ErrorResponse {
        val violations = ex.bindingResult.fieldErrors
            .joinToString("; ") { "'${it.field}' ${it.defaultMessage}" }
        return ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = violations.ifBlank { "Request validation failed" },
        )
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    fun handleMaxUploadSize(): ErrorResponse = ErrorResponse(
        status = HttpStatus.PAYLOAD_TOO_LARGE.value(),
        error = HttpStatus.PAYLOAD_TOO_LARGE.reasonPhrase,
        message = "Uploaded file exceeds the maximum allowed size",
    )
}
