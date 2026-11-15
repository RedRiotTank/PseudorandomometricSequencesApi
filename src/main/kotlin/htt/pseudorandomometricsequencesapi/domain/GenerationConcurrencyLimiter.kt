package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore

/**
 * Thrown when the server is already generating the maximum configured number of values.
 */
class GenerationCapacityExceededException(message: String) : RuntimeException(message)

/**
 * Bounds the total number of scalar values being generated at once.
 *
 * <p>A plain request-count semaphore would reject harmless bursts of tiny requests. This
 * limiter instead charges each request for the number of values it is about to produce
 * (`count`, `count × variables`, or `count × dimension`), which tracks both CPU and heap
 * usage. A single request larger than the whole pool is allowed to run alone.</p>
 */
@Component
class GenerationConcurrencyLimiter(
    private val limits: ApiLimitsProperties
) {

    private val semaphore = Semaphore(limits.maxConcurrentValues, true)

    fun <T> withPermit(workUnits: Long, action: () -> T): T {
        val permits = workUnits
            .coerceAtLeast(1L)
            .coerceAtMost(limits.maxConcurrentValues.toLong())
            .toInt()

        if (!semaphore.tryAcquire(permits)) {
            throw GenerationCapacityExceededException(
                "The service is at capacity with other generation requests. " +
                    "Please retry in a few seconds or reduce 'count'."
            )
        }

        return try {
            action()
        } finally {
            semaphore.release(permits)
        }
    }
}
