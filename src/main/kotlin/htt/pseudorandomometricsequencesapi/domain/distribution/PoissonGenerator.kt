package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.PoissonDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Poisson_distribution">Poisson distribution</a>}.
 *
 * <p>This is a **discrete** probability distribution that expresses the probability of a given number
 * of events occurring in a fixed interval of time or space if these events occur with a known constant
 * mean rate and independently of the time since the last event.</p>
 *
 * @param mean The mean ($\lambda$) number of events per interval. Must be strictly positive ($\lambda > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 */
class PoissonGenerator(
    mean: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** The internal Poisson distribution object from Apache Commons Math. */
    val distribution: PoissonDistribution

    init {
        require(mean > 0.0) { "Poisson 'mean' (param1) must be strictly positive (lambda > 0)." }

        this.distribution = PoissonDistribution(
            randomGenerator,
            mean,
            PoissonDistribution.DEFAULT_EPSILON,
            PoissonDistribution.DEFAULT_MAX_ITERATIONS
        )
    }

    /**
     * Generates and returns the next pseudorandom sample from the Poisson distribution.
     *
     * @return A pseudorandom {@code Double} value (which is an integer) sampled from the Poisson distribution.
     */
    override fun sample(): Double {
        return distribution.sample().toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "poisson"

        /**
         * Factory method to create an instance of {@code PoissonGenerator}.
         *
         * <p>Default value:</p>
         * <ul>
         * <li>{@code param1} ($\lambda$): Defaults to **1.0**.</li>
         * </ul>
         *
         * @param param1 The desired mean ($\lambda$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance.
         * @return A new {@code PoissonGenerator} instance.
         */
        fun create(param1: Double?, generator: RandomGenerator): PoissonGenerator {
            val lambda = param1 ?: 1.0
            return PoissonGenerator(lambda, generator)
        }
    }
}