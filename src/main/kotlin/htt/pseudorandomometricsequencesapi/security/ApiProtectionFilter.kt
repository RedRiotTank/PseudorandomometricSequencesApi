package htt.pseudorandomometricsequencesapi.security

import com.fasterxml.jackson.databind.ObjectMapper
import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Protects the generation endpoints from accidental and malicious overload.
 *
 * <p>Two independent guards are applied before a request reaches a controller:</p>
 *
 * <ol>
 *   <li><b>Token-bucket rate limiting</b> — one global bucket and one bucket per client
 *       IP. Rejections use HTTP 429 with `Retry-After`.</li>
 *   <li><b>Body size limiting</b> — oversized POST bodies get HTTP 413, including chunked
 *       requests without a `Content-Length` header.</li>
 * </ol>
 *
 * <p>Generation itself is additionally bounded by [htt.pseudorandomometricsequencesapi.domain.GenerationConcurrencyLimiter],
 * which charges each request for the number of values it will produce, and by the adaptive
 * per-request time budget in [htt.pseudorandomometricsequencesapi.domain.GenerationBudget].</p>
 *
 * <p>Only `/api/v1/random/` is protected; documentation and the static distribution
 * catalog remain freely accessible.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class ApiProtectionFilter(
    private val limits: ApiLimitsProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiProtectionFilter::class.java)
        private const val PROTECTED_PREFIX = "/api/v1/random/"
        private val CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1)
        private val IP_BUCKET_TTL_NANOS = TimeUnit.MINUTES.toNanos(10)
        private const val MAX_TRACKED_IPS = 20_000
    }

    private val globalLimiter = TokenBucket(
        limits.maxRequestsPerMinute,
        limits.maxRequestsPerMinute / 60.0
    )
    private val perIpBuckets = ConcurrentHashMap<String, IpBucket>()
    private val lastCleanup = AtomicLong(System.nanoTime())

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path == null || !path.startsWith(PROTECTED_PREFIX)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        response.setHeader("X-Content-Type-Options", "nosniff")

        if (!globalLimiter.tryAcquire()) {
            reject(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "The service is receiving too many requests. Please retry later.",
                globalLimiter.secondsUntilNextToken()
            )
            return
        }

        val clientIp = request.remoteAddr ?: "unknown"
        cleanupBucketsIfNeeded()

        val now = System.nanoTime()
        val ipBucket = perIpBuckets.computeIfAbsent(clientIp) {
            IpBucket(
                TokenBucket(
                    limits.maxRequestsPerMinutePerIp,
                    limits.maxRequestsPerMinutePerIp / 60.0
                ),
                now
            )
        }
        ipBucket.lastSeenNanos = now

        if (!ipBucket.limiter.tryAcquire()) {
            reject(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests from this client. Please retry later.",
                ipBucket.limiter.secondsUntilNextToken()
            )
            return
        }

        if (request.method.equals("POST", ignoreCase = true) &&
            request.contentLengthLong > limits.maxRequestBodyBytes
        ) {
            reject(
                response,
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Request body exceeds the maximum allowed size of " +
                    "${limits.maxRequestBodyBytes} bytes.",
                retryAfterSeconds = null
            )
            return
        }

        try {
            val effectiveRequest = if (
                request.method.equals("POST", ignoreCase = true) &&
                request.contentLengthLong < 0
            ) {
                LimitedBodyRequest(request, limits.maxRequestBodyBytes)
            } else {
                request
            }
            filterChain.doFilter(effectiveRequest, response)
        } catch (ex: RequestTooLargeException) {
            if (!response.isCommitted) {
                reject(
                    response,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    ex.message ?: "Request body is too large.",
                    retryAfterSeconds = null
                )
            }
        }
    }

    private fun cleanupBucketsIfNeeded() {
        val now = System.nanoTime()
        val previous = lastCleanup.get()
        if (now - previous < CLEANUP_INTERVAL_NANOS) {
            return
        }
        if (!lastCleanup.compareAndSet(previous, now)) {
            return
        }

        perIpBuckets.entries.removeIf { now - it.value.lastSeenNanos > IP_BUCKET_TTL_NANOS }

        // Defensive bound in case an attacker rotates source addresses. The global
        // bucket still protects the service if this map is cleared.
        if (perIpBuckets.size > MAX_TRACKED_IPS) {
            logger.warn("Clearing per-IP rate-limit buckets after reaching ${perIpBuckets.size} entries")
            perIpBuckets.clear()
        }
    }

    private fun reject(
        response: HttpServletResponse,
        status: HttpStatus,
        message: String,
        retryAfterSeconds: Int?
    ) {
        if (response.isCommitted) {
            return
        }
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        retryAfterSeconds?.let { response.setHeader(HttpHeaders.RETRY_AFTER, it.toString()) }

        try {
            objectMapper.writeValue(
                response.writer,
                ProtectionError(
                    status = status.value(),
                    error = status.reasonPhrase,
                    message = message,
                    retryAfterSeconds = retryAfterSeconds
                )
            )
        } catch (ex: Exception) {
            logger.warn("Unable to write protection error response: ${ex.message}")
        }
    }

    private class IpBucket(
        val limiter: TokenBucket,
        @Volatile var lastSeenNanos: Long
    )

    private class LimitedBodyRequest(
        request: HttpServletRequest,
        private val maxBytes: Long
    ) : HttpServletRequestWrapper(request) {

        override fun getInputStream(): ServletInputStream =
            LimitedServletInputStream(super.getInputStream(), maxBytes)
    }

    private class LimitedServletInputStream(
        private val delegate: ServletInputStream,
        private val maxBytes: Long
    ) : ServletInputStream() {

        private var bytesRead = 0L

        override fun read(): Int {
            val value = delegate.read()
            if (value >= 0) {
                addBytes(1)
            }
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val read = delegate.read(buffer, offset, length)
            if (read > 0) {
                addBytes(read)
            }
            return read
        }

        override fun available(): Int = delegate.available()

        override fun isFinished(): Boolean = delegate.isFinished

        override fun isReady(): Boolean = delegate.isReady

        override fun setReadListener(readListener: ReadListener) =
            delegate.setReadListener(readListener)

        private fun addBytes(count: Int) {
            bytesRead += count
            if (bytesRead > maxBytes) {
                throw RequestTooLargeException(maxBytes)
            }
        }
    }
}

/**
 * Error payload returned by [ApiProtectionFilter] when a request is rejected before it
 * reaches a controller.
 */
data class ProtectionError(
    val status: Int,
    val error: String,
    val message: String,
    val retryAfterSeconds: Int? = null
)
