package htt.pseudorandomometricsequencesapi.domain.distribution

import java.util.Random
import kotlin.math.ln

/**
 * Represents a generator for pseudorandom numbers following an
 * {@link <a href="https://en.wikipedia.org/wiki/Exponential_distribution">Exponential distribution</a>}.
 *
 * <p>This class implements the {@code SequenceGenerator} interface and uses the
 * <b>Inverse Transform Sampling</b> method to generate samples from a standard uniform
 * deviate.</p>
 *
 * <p>The Exponential distribution describes the time between events in a
 * <a href="https://en.wikipedia.org/wiki/Poisson_process">Poisson point process</a>.
 * It is characterized by a single parameter, $\lambda$, the <b>rate parameter</b>.</p>
 *
 * <p>The probability density function (PDF) for a non-negative $x$ is:
 * $$ f(x; \lambda) = \lambda e^{-\lambda x} \quad \text{for } x \ge 0 $$</p>
 *
 * @param lambda The <b>rate parameter</b> ($\lambda$) of the distribution. It is the
 * reciprocal of the mean, $E[X] = 1/\lambda$. Must be positive ($\lambda > 0$).
 * @param javaRandom The underlying {@code java.util.Random} instance used to generate
 * the base uniform pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Exponential_distribution">Exponential Distribution</a>
 * @see java.util.Random#nextDouble()
 */
class ExponentialGenerator(
    private val lambda: Double,
    private val javaRandom: Random
) : SequenceGenerator {

    /**
     * Initializes the generator and validates the rate parameter.
     *
     * @throws IllegalArgumentException if {@code lambda} is not positive ($\lambda \le 0$).
     */
    init {
        require(lambda > 0.0) { "Exponential 'lambda' (param1) must be positive." }
    }

    /**
     * Generates and returns the next pseudorandom sample from the Exponential distribution
     * with rate $\lambda$.
     *
     * <p>The sample is generated using the Inverse Transform method based on the inverse
     * of the cumulative distribution function (CDF), which is $F^{-1}(U) = -\frac{1}{\lambda} \ln(1 - U)$,
     * where $U \sim \text{Uniform}(0, 1)$.</p>
     *
     * <p>Note: The expression {@code 1.0 - javaRandom.nextDouble()} ensures $U \in (0, 1]$ and
     * is equivalent to {@code javaRandom.nextDouble()} in terms of distribution, which
     * yields the simpler formula:
     * $$ X = -\frac{1}{\lambda} \ln(U) $$
     * The generated value $X$ is non-negative ($X \ge 0$).</p>
     *
     * @return A pseudorandom {@code Double} value sampled from $\text{Exp}(\lambda)$.
     */
    override fun sample(): Double {
        return -1.0 / lambda * ln(1.0 - javaRandom.nextDouble())
    }

    /**
     * Contains constants and factory methods for {@code ExponentialGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "exponential".
         */
        const val DISTRIBUTION_NAME = "exponential"

        /**
         * Factory method to create an instance of {@code ExponentialGenerator}.
         *
         * <p>It applies a default value if the parameter is {@code null}:</p>
         * <ul>
         * <li>{@code param1} (lambda, $\lambda$): Defaults to <b>1.0</b>.</li>
         * </ul>
         * <p>Using the default value results in the <b>Standard Exponential Distribution</b>,
         * $\text{Exp}(1)$, which has a mean of 1.</p>
         *
         * @param param1 The desired rate parameter ($\lambda$) for the distribution (can be {@code null}).
         * @param javaRandom The {@code java.util.Random} instance to be used by the generator.
         * @return A new {@code ExponentialGenerator} instance.
         * @throws IllegalArgumentException if the effective {@code lambda} is not positive.
         */
        fun create(param1: Double?, javaRandom: Random): ExponentialGenerator {
            val lambda = param1 ?: 1.0

            return ExponentialGenerator(lambda, javaRandom)
        }
    }
}