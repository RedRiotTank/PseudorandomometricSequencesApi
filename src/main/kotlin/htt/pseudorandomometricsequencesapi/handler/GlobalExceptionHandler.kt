package htt.pseudorandomometricsequencesapi.handler

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.resource.NoResourceFoundException
import htt.pseudorandomometricsequencesapi.domain.GenerationBudgetExceededException
import htt.pseudorandomometricsequencesapi.domain.GenerationCapacityExceededException
import htt.pseudorandomometricsequencesapi.security.RequestTooLargeException

/**
 * Global exception handler configured using [@ControllerAdvice][org.springframework.web.bind.annotation.ControllerAdvice].
 *
 * <p>This class centralizes error handling across all controllers in the application,
 * ensuring standardized and structured JSON responses for various exceptions.</p>
 */
@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handles exceptions caused by invalid user input or business logic violations,
     * typically thrown via Kotlin's `require()` or `check()` functions.
     *
     * <p>This method maps [IllegalArgumentException] instances to an **HTTP 400 Bad Request**
     * status, providing the client with clear feedback on invalid parameters.</p>
     *
     * @param ex The [IllegalArgumentException] that was thrown.
     * @param request The current [WebRequest] providing context about the request.
     * @return A [ResponseEntity] containing [ErrorDetails] and an HTTP 400 status.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseBody
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {

        logger.warn("BAD REQUEST (400) in {}. Message: {}",
            request.getDescription(false), ex.message)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = ex.message ?: "Invalid request parameters provided.",
            details = request.getDescription(false)
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    /**
     * Handles malformed or missing JSON request bodies.
     *
     * @param ex The [HttpMessageNotReadableException] that was thrown.
     * @param request The current [WebRequest] providing context about the request.
     * @return A [ResponseEntity] containing [ErrorDetails] and an HTTP 400 status.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseBody
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {

        if (ex.causeChainContains<RequestTooLargeException>()) {
            return requestTooLargeResponse(ex, request)
        }

        logger.warn("BAD REQUEST (400) in {}. Message: {}",
            request.getDescription(false), ex.message)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = "Invalid or missing request body.",
            details = request.getDescription(false)
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    /**
     * Handles bodies that exceed the configured maximum size. The protection filter
     * usually catches this first, but Jackson may wrap the exception while parsing.
     */
    @ExceptionHandler(RequestTooLargeException::class)
    @ResponseBody
    fun handleRequestTooLargeException(
        ex: RequestTooLargeException,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> = requestTooLargeResponse(ex, request)

    /**
     * Handles valid requests whose projected generation time exceeds the configured
     * per-request budget. Clients should reduce `count` or choose a cheaper distribution.
     */
    @ExceptionHandler(GenerationBudgetExceededException::class)
    @ResponseBody
    fun handleGenerationBudgetExceededException(
        ex: GenerationBudgetExceededException,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {

        logger.warn("REQUEST TOO EXPENSIVE (422) in {}. Message: {}",
            request.getDescription(false), ex.message)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = ex.message ?: "The requested generation is too expensive.",
            details = request.getDescription(false)
        )

        return ResponseEntity(errorDetails, HttpStatus.UNPROCESSABLE_ENTITY)
    }

    @ExceptionHandler(GenerationCapacityExceededException::class)
    @ResponseBody
    fun handleGenerationCapacityExceededException(
        ex: GenerationCapacityExceededException,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {

        logger.warn("SERVICE AT CAPACITY (429) in {}. Message: {}",
            request.getDescription(false), ex.message)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = ex.message ?: "The service is at capacity.",
            details = request.getDescription(false)
        )

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, "2")
            .body(errorDetails)
    }

    private fun requestTooLargeResponse(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {
        logger.warn("PAYLOAD TOO LARGE (413) in {}. Message: {}",
            request.getDescription(false), ex.message)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = ex.message ?: "Request body is too large.",
            details = request.getDescription(false)
        )

        return ResponseEntity(errorDetails, HttpStatus.PAYLOAD_TOO_LARGE)
    }

    private inline fun <reified T : Throwable> Throwable.causeChainContains(): Boolean {
        var current: Throwable? = this
        var depth = 0
        while (current != null && depth < 32) {
            if (current is T) {
                return true
            }
            current = current.cause
            depth++
        }
        return false
    }

    /**
     * A comprehensive handler that serves as a catch-all for any unhandled
     * [Exception] not covered by other specific handlers.
     *
     * <p>This method logs the stack trace and maps all uncaught exceptions to an
     * **HTTP 500 Internal Server Error**, preventing internal server details from
     * leaking to the client via a raw stack trace.</p>
     *
     * @param ex The unhandled [Exception] that was thrown.
     * @param request The current [WebRequest] providing context about the request.
     * @return A [ResponseEntity] containing [ErrorDetails] and an HTTP 500 status.
     */
    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun handleAllExceptions(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {

        logger.error("!!! INTERNAL SERVER ERROR (500) in {}. Unexpected error: {}",
            request.getDescription(false), ex.message, ex)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = "An unexpected error occurred on the server.",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * Handles requests for non-existent resources or endpoints.
     *
     * @param ex The [NoResourceFoundException] that was thrown.
     * @param request The current [WebRequest] providing context about the request.
     * @return A [ResponseEntity] containing [ErrorDetails] and an HTTP 404 status.
     */
    @ExceptionHandler(NoResourceFoundException::class)
    @ResponseBody
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorDetails> {

        logger.warn("NOT FOUND (404) in {}. Message: {}",
            request.getDescription(false), ex.message)

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = "The requested resource was not found.",
            details = request.getDescription(false)
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails)
    }
}

/**
 * Data structure used to standardize the format of error responses returned by the API.
 *
 * @property timestamp The exact time the error occurred.
 * @property message A descriptive error message, often taken from the exception itself.
 * @property details Additional information about the request, such as the URI path.
 */
data class ErrorDetails(
    val timestamp: LocalDateTime,
    val message: String,
    val details: String
)