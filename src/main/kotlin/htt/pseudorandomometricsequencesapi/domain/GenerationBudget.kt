package htt.pseudorandomometricsequencesapi.domain

/**
 * Thrown when a generation request is valid but would take too long for the
 * configured per-request budget.
 */
class GenerationBudgetExceededException(message: String) : RuntimeException(message)

/**
 * Enforces a wall-clock budget on a generation request without changing the generated
 * values.
 *
 * <p>Each plan samples a small prefix with the real generator and extrapolates the cost of
 * the full sequence. If the sum of the extrapolations exceeds [maxNanos], the request is
 * rejected before the expensive part runs. The prefix is retained in the plan, so accepted
 * requests continue from the same random state and remain bit-for-bit reproducible.</p>
 *
 * <p>The budget also checks the actual elapsed time while the remaining samples are
 * generated, which bounds the damage if the initial estimate was too optimistic.</p>
 */
class GenerationBudget(
    private val maxNanos: Long,
    private val calibrationSamples: Int
) {

    private val startedAt = System.nanoTime()
    private var reservedNanos = 0L

    fun <T> plan(count: Int, label: String, sample: () -> T): SamplePlan<T> {
        val plan = SamplePlan(count, calibrationSamples, label, sample)
        plan.calibrate(this)

        val projectedTotal = reservedNanos + plan.projectedNanos
        if (projectedTotal > maxNanos || elapsedNanos() > maxNanos) {
            throw GenerationBudgetExceededException(
                budgetMessage(label, plan.projectedNanos, projectedTotal)
            )
        }

        reservedNanos = projectedTotal
        return plan
    }

    fun ensureWithinDeadline(label: String) {
        val elapsed = elapsedNanos()
        if (elapsed > maxNanos) {
            throw GenerationBudgetExceededException(
                "Generation exceeded the ${maxMillis()} ms request budget while sampling $label. " +
                    "Reduce 'count' or choose a cheaper distribution."
            )
        }
    }

    private fun budgetMessage(label: String, projectedNanos: Long, projectedTotal: Long): String {
        val estimated = if (projectedTotal > 0) projectedTotal else projectedNanos
        return "Request too expensive: estimated generation time for $label is " +
            "${estimated / 1_000_000} ms, above the ${maxMillis()} ms budget. " +
            "Reduce 'count' or choose a cheaper distribution."
    }

    private fun maxMillis(): Long = maxNanos / 1_000_000

    fun elapsedNanos(): Long = System.nanoTime() - startedAt
}

/**
 * One sequence inside a [GenerationBudget]. The prefix sampled during calibration is
 * part of the final sequence.
 */
class SamplePlan<T> internal constructor(
    private val count: Int,
    private val calibrationSamples: Int,
    private val label: String,
    private val sample: () -> T
) {

    private val prefix = ArrayList<T>(minOf(count, calibrationSamples))
    private var measuredNanos = 0L

    var projectedNanos: Long = 0L
        private set

    fun calibrate(budget: GenerationBudget) {
        val warmup = minOf(count, calibrationSamples)
        val start = System.nanoTime()
        for (index in 0 until warmup) {
            prefix.add(sample())
            if (index and CALIBRATION_CHECK_MASK == 0) {
                budget.ensureWithinDeadline(label)
            }
        }
        measuredNanos = (System.nanoTime() - start).coerceAtLeast(0L)

        projectedNanos = if (warmup >= count) {
            measuredNanos
        } else {
            val projected = measuredNanos.toDouble() * count.toDouble() / warmup.toDouble()
            if (projected >= Long.MAX_VALUE.toDouble()) Long.MAX_VALUE else projected.toLong()
        }
    }

    fun finish(budget: GenerationBudget): List<T> {
        val values = ArrayList<T>(count)
        values.addAll(prefix)

        var generated = prefix.size
        while (generated < count) {
            values.add(sample())
            generated++
            if (generated and DEADLINE_CHECK_MASK == 0) {
                budget.ensureWithinDeadline(label)
            }
        }

        budget.ensureWithinDeadline(label)
        return values
    }

    private companion object {
        const val CALIBRATION_CHECK_MASK = 0x3F
        const val DEADLINE_CHECK_MASK = 0xFF
    }
}
