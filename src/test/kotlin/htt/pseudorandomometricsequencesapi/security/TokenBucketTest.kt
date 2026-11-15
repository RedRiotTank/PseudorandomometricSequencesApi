package htt.pseudorandomometricsequencesapi.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenBucketTest {

    @Test
    fun `should allow a burst up to capacity and then reject`() {
        val bucket = TokenBucket(capacity = 3, refillPerSecond = 1.0)

        assertTrue(bucket.tryAcquire())
        assertTrue(bucket.tryAcquire())
        assertTrue(bucket.tryAcquire())
        assertFalse(bucket.tryAcquire())
    }

    @Test
    fun `should refill over time`() {
        val bucket = TokenBucket(capacity = 1, refillPerSecond = 100.0)

        assertTrue(bucket.tryAcquire())
        assertFalse(bucket.tryAcquire())

        Thread.sleep(30)
        assertTrue(bucket.tryAcquire())
    }

    @Test
    fun `should report a retry delay when empty`() {
        val bucket = TokenBucket(capacity = 1, refillPerSecond = 1.0)

        assertTrue(bucket.tryAcquire())
        assertEquals(1, bucket.secondsUntilNextToken())
    }
}
