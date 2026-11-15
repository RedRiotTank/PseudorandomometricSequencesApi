package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GenerationConcurrencyLimiterTest {

    private fun limiter(maxValues: Int): GenerationConcurrencyLimiter {
        val properties = ApiLimitsProperties().apply {
            maxConcurrentValues = maxValues
        }
        return GenerationConcurrencyLimiter(properties)
    }

    @Test
    fun `should reject a request that does not fit in the remaining pool`() {
        val limiter = limiter(10)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)

        val worker = Thread {
            limiter.withPermit(6) {
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
        }.apply { start() }

        assertTrue(entered.await(5, TimeUnit.SECONDS))

        val exception = assertThrows<GenerationCapacityExceededException> {
            limiter.withPermit(6) { }
        }
        assertTrue(exception.message!!.contains("capacity"))

        assertEquals(7, limiter.withPermit(4) { 7 })

        release.countDown()
        worker.join(5_000)
    }

    @Test
    fun `should allow a single request larger than the pool to run alone`() {
        val limiter = limiter(10)

        assertEquals("ok", limiter.withPermit(1_000) { "ok" })
        assertEquals("again", limiter.withPermit(10) { "again" })
    }

    @Test
    fun `should release permits when generation fails`() {
        val limiter = limiter(10)

        val exception = assertThrows<IllegalStateException> {
            limiter.withPermit(10) { error("boom") }
        }
        assertEquals("boom", exception.message)

        assertEquals("ok", limiter.withPermit(10) { "ok" })
    }
}
