package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.TriangularDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Triangular_distribution">Triangular distribution</a>}.
 *
 * <p>This continuous probability distribution is defined by a minimum, maximum, and mode (peak).
 * It is often used in business decision making and project planning (like PERT or Monte Carlo
 * simulations) when exact data is limited but the minimum, maximum, and most likely values are known.</p>
 *
 * @param a The minimum value (lower limit).
 * @param c The mode (most likely value or peak).
 * @param b The maximum value (upper limit).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}.
 */
class TriangularGenerator(
    a: Double,
    c: Double,
    b: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** The internal Triangular distribution object from Apache Commons Math. */
    val distribution: TriangularDistribution

    init {
        require(a < b) { "Triangular 'min' (param1) must be strictly less than 'max' (param3)." }
        require(c in a..b) { "Triangular 'mode' (param2) must be between 'min' and 'max' inclusive." }

        this.distribution = TriangularDistribution(randomGenerator, a, c, b)
    }

    override fun sample(): Double {
        return distribution.sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "triangular"

        /**
         * Factory method to create an instance of {@code TriangularGenerator}.
         *
         * <p>Default values:</p>
         * <ul>
         * <li>{@code param1} ($a$, min): Defaults to **0.0**.</li>
         * <li>{@code param2} ($c$, mode): Defaults to **0.5**.</li>
         * <li>{@code param3} ($b$, max): Defaults to **1.0**.</li>
         * </ul>
         *
         * @param param1 The desired minimum ($a$) (can be {@code null}).
         * @param param2 The desired mode ($c$) (can be {@code null}).
         * @param param3 The desired maximum ($b$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance.
         */
        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): TriangularGenerator {
            val min = param1 ?: 0.0
            val mode = param2 ?: 0.5
            val max = param3 ?: 1.0

            return TriangularGenerator(min, mode, max, generator)
        }
    }
}