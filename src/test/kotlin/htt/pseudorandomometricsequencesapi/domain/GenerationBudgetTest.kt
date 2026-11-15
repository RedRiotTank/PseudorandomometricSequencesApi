package htt.pseudorandomometricsequencesapi.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GenerationBudgetTest {

    @Test
    fun `should keep the calibration prefix and preserve the full sequence`() {
        val counter = AtomicInteger()
        val budget = GenerationBudget(TimeUnit.SECONDS.toNanos(10), calibrationSamples = 5)

        val plan = budget.plan(50, "test") { counter.getAndIncrement().toDouble() }

        assertEquals((0 until 50).map { it.toDouble() }, plan.finish(budget))
        assertEquals(50, counter.get())
    }

    @Test
    fun `should generate small requests that fit entirely in the calibration prefix`() {
        val budget = GenerationBudget(TimeUnit.SECONDS.toNanos(10), calibrationSamples = 100)

        val plan = budget.plan(3, "test") { 1.0 }

        assertEquals(3, plan.finish(budget).size)
    }

    @Test
    fun `should reject a projection above the budget before generating everything`() {
        // The budget is deliberately much larger than the calibration time so the
        // projection (count / calibrationSamples x measured time) is what trips the
        // limit. A tiny budget would make the test depend on scheduler latency.
        val budget = GenerationBudget(TimeUnit.SECONDS.toNanos(2), calibrationSamples = 3)

        val exception = assertThrows<GenerationBudgetExceededException> {
            budget.plan(10_000, "slow distribution") {
                Thread.sleep(2)
                1.0
            }
        }

        assertEquals(true, exception.message!!.contains("Request too expensive"))
    }

    @Test
    fun `should enforce the aggregate budget across multiple plans`() {
        // Each plan finishes inside the calibration prefix, so its projection is its
        // measured time (~10 ms). Individually they fit, but ten of them exceed the
        // 60 ms aggregate budget.
        val budget = GenerationBudget(TimeUnit.MILLISECONDS.toNanos(60), calibrationSamples = 10)

        assertThrows<GenerationBudgetExceededException> {
            repeat(10) {
                budget.plan(10, "variable $it") {
                    Thread.sleep(1)
                    1.0
                }
            }
        }
    }
}
