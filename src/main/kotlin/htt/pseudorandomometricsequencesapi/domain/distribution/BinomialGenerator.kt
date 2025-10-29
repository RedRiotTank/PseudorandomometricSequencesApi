package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.BinomialDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Binomial_distribution">Binomial distribution</a>}.
 *
 * <p>This is a **discrete** distribution that models the number of successes in a fixed number
 * of independent Bernoulli trials. The generated values are integers $X \in [0, n]$.</p>
 *
 * @param trials The number of independent trials ($n$). Must be a positive integer.
 * @param probabilityOfSuccess The probability of success in each trial ($p$). Must be $0 \le p \le 1$.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the Binomial distribution.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Binomial_distribution">Binomial Distribution</a>
 * @see org.apache.commons.math3.distribution.BinomialDistribution
 */
class BinomialGenerator(
    trials: Int,
    probabilityOfSuccess: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** The internal Binomial distribution object from Apache Commons Math. */
    val distribution: BinomialDistribution = BinomialDistribution(randomGenerator, trials, probabilityOfSuccess)

    /**
     * Generates and returns the next pseudorandom sample from the Binomial distribution.
     *
     * <p>The sample is an integer, but is returned as a {@code Double} to comply with the
     * {@code SequenceGenerator} interface.</p>
     *
     * @return A pseudorandom {@code Double} value (which is an integer) sampled from the Binomial distribution.
     */
    override fun sample(): Double {
        return distribution.sample().toDouble()
    }

    /** Contains constants and factory methods for {@code BinomialGenerator}. */
    companion object {
        const val DISTRIBUTION_NAME = "binomial"

        /**
         * Factory method to create an instance of {@code BinomialGenerator}.
         *
         * <p>Default values:</p>
         * <ul>
         * <li>{@code param1} (trials, $n$): Defaults to **10.0** (converted to Int).</li>
         * <li>{@code param2} (probability of success, $p$): Defaults to **0.5**.</li>
         * </ul>
         *
         * @param param1 The number of trials ($n$) (can be {@code null}).
         * @param param2 The probability of success ($p$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance.
         * @return A new {@code BinomialGenerator} instance.
         * @throws IllegalArgumentException if $n \le 0$ or $p \notin [0, 1]$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): BinomialGenerator {
            val trialsDouble = param1 ?: 10.0
            val probability = param2 ?: 0.5

            require(trialsDouble > 0 && trialsDouble == trialsDouble.toInt().toDouble()) {
                "Binomial 'trials' (param1) must be a positive integer."
            }
            require(probability in 0.0..1.0) {
                "Binomial 'probability' (param2) must be between 0.0 and 1.0."
            }

            return BinomialGenerator(trialsDouble.toInt(), probability, generator)
        }
    }
}