package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Chi-squared_distribution">Chi-squared distribution</a>}.
 *
 * <p>This continuous probability distribution is widely used in inferential statistics,
 * particularly in hypothesis testing (e.g., Pearson's chi-squared test) and for constructing
 * confidence intervals for population variances.</p>
 *
 * <p>The distribution is parameterized solely by its degrees of freedom ($k$).</p>
 *
 * @param degreesOfFreedom The **degrees of freedom** ($k$). Must be strictly positive ($k > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}.
 */
class ChiSquaredGenerator(
    degreesOfFreedom: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** The internal Chi-Squared distribution object from Apache Commons Math. */
    val distribution: ChiSquaredDistribution

    init {
        require(degreesOfFreedom > 0.0) { "Chi-Squared 'degreesOfFreedom' (param1) must be strictly positive (k > 0)." }
        this.distribution = ChiSquaredDistribution(randomGenerator, degreesOfFreedom)
    }

    override fun sample(): Double {
        return distribution.sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "chi-squared"

        /**
         * Factory method to create an instance of {@code ChiSquaredGenerator}.
         *
         * <p>Default value:</p>
         * <ul>
         * <li>{@code param1} ($k$, degrees of freedom): Defaults to **1.0**.</li>
         * </ul>
         *
         * @param param1 The desired degrees of freedom ($k$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance.
         */
        fun create(param1: Double?, generator: RandomGenerator): ChiSquaredGenerator {
            val degreesOfFreedom = param1 ?: 1.0
            return ChiSquaredGenerator(degreesOfFreedom, generator)
        }
    }
}