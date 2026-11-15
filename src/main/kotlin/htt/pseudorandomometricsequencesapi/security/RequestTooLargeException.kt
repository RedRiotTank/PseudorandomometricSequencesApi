package htt.pseudorandomometricsequencesapi.security

/**
 * Raised when a POST body exceeds the configured maximum size. It is handled both by
 * [ApiProtectionFilter] and, when Jackson wraps it while parsing, by the global
 * exception handler.
 */
class RequestTooLargeException(maxBytes: Long) : RuntimeException(
    "Request body exceeds the maximum allowed size of $maxBytes bytes."
)
