package htt.pseudorandomometricsequencesapi.handler

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * Global exception handler configured using [@ControllerAdvice][org.springframework.web.bind.annotation.ControllerAdvice].
 *
 * <p>This class centralizes error handling across all controllers in the application,
 * ensuring standardized and structured JSON responses for various exceptions.</p>
 */
@ControllerAdvice
class GlobalExceptionHandler {

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
        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = ex.message ?: "Invalid request parameters provided.",
            details = request.getDescription(false)
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
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
        // Log the full error for server-side debugging
        ex.printStackTrace()

        val errorDetails = ErrorDetails(
            timestamp = LocalDateTime.now(),
            message = "An unexpected error occurred on the server: ${ex.message}",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
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