package htt.pseudorandomometricsequencesapi.security

import kotlin.math.ceil

/**
 * Small, dependency-free token bucket used by the request protection filter.
 *
 * <p>The bucket starts full, which allows a short burst up to [capacity], and then
 * refills at [refillPerSecond]. Thread-safe.</p>
 */
class TokenBucket(
    private val capacity: Int,
    private val refillPerSecond: Double
) {

    private var tokens: Double = capacity.toDouble()
    private var lastRefillNanos: Long = System.nanoTime()

    @Synchronized
    fun tryAcquire(): Boolean {
        refill()
        if (tokens < 1.0) {
            return false
        }
        tokens -= 1.0
        return true
    }

    @Synchronized
    fun secondsUntilNextToken(): Int {
        refill()
        if (tokens >= 1.0) {
            return 0
        }
        val seconds = ceil((1.0 - tokens) / refillPerSecond).toInt()
        return seconds.coerceAtLeast(1)
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0
        if (elapsedSeconds <= 0.0) {
            return
        }
        tokens = (tokens + elapsedSeconds * refillPerSecond).coerceAtMost(capacity.toDouble())
        lastRefillNanos = now
    }
}
