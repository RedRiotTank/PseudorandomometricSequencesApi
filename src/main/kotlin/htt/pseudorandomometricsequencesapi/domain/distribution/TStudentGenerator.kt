package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.TDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Student%27s_t-distribution">Student's t-distribution</a>}.
 *
 * <p>This distribution is characterized by its heavy tails compared to the Normal distribution,
 * making it suitable for modeling data with more frequent extreme values.</p>
 *
 * <p>The distribution is parameterized solely by its degrees of freedom ($\nu$).</p>
 *
 * @param degreesOfFreedom The **degrees of freedom** ($\nu$). Must be positive ($\nu > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the T distribution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Student%27s_t-distribution">Student's t-distribution</a>
 * @see org.apache.commons.math3.distribution.TDistribution
 */
class TStudentGenerator(
    degreesOfFreedom: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** The internal T distribution object from Apache Commons Math. */
    val distribution: TDistribution

    init {
        require(degreesOfFreedom > 0.0) { "T-Student 'degreesOfFreedom' (param1) must be positive (ν > 0)." }

        this.distribution = TDistribution(randomGenerator, degreesOfFreedom)
    }

    /**
     * Generates and returns the next pseudorandom sample from the T-Student distribution.
     *
     * @return A pseudorandom {@code Double} value sampled from the T-Student distribution.
     */
    override fun sample(): Double {
        return distribution.sample()
    }

    /** Contains constants and factory methods for {@code TStudentGenerator}. */
    companion object {
        const val DISTRIBUTION_NAME = "t-student"

        /**
         * Factory method to create an instance of {@code TStudentGenerator}.
         *
         * <p>Default value:</p>
         * <ul>
         * <li>{@code param1} ($\nu$): Defaults to **10.0**.</li>
         * </ul>
         *
         * @param param1 The desired degrees of freedom ($\nu$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance.
         * @return A new {@code TStudentGenerator} instance.
         */
        fun create(param1: Double?, generator: RandomGenerator): TStudentGenerator {
            val degreesOfFreedom = param1 ?: 10.0

            return TStudentGenerator(degreesOfFreedom, generator)
        }
    }
}