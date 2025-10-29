package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.WeibullDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Weibull_distribution">Weibull distribution</a>}.
 *
 * <p>This distribution is widely used in reliability engineering, modeling the
 * time until failure of a system or component, and in extreme value theory.
 * The Weibull distribution is defined over the range $x \ge 0$.</p>
 *
 * <p>The probability density function (PDF) for $x \ge 0$ is:
 * $$ f(x; k, \lambda) = \frac{k}{\lambda} \left(\frac{x}{\lambda}\right)^{k-1} e^{-(x/\lambda)^k} $$
 * where $k$ is the shape parameter and $\lambda$ is the scale parameter.</p>
 *
 * @param shape The **shape parameter** ($k$). This value determines the form of the distribution.
 * Must be positive ($k > 0$). If $k=1$, the distribution simplifies to the Exponential distribution.
 * @param scale The **scale parameter** ($\lambda$). This value relates to the characteristic life.
 * Must be positive ($\lambda > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the Weibull distribution for generating base pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Weibull_distribution">Weibull Distribution</a>
 * @see org.apache.commons.math3.distribution.WeibullDistribution
 */
class WeibullGenerator(
    shape: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * The internal Weibull distribution object from Apache Commons Math.
     */
    val distribution: WeibullDistribution

    /**
     * Initializes the generator and validates the shape and scale parameters.
     *
     * @throws IllegalArgumentException if {@code shape} ($k$) or {@code scale} ($\lambda$) are not positive.
     */
    init {
        require(shape > 0.0) { "Weibull 'shape' (param1) must be positive (k > 0)." }
        require(scale > 0.0) { "Weibull 'scale' (param2) must be positive (lambda > 0)." }

        this.distribution = WeibullDistribution(randomGenerator, shape, scale)
    }

    /**
     * Generates and returns the next pseudorandom sample from the Weibull distribution.
     *
     * <p>The generated value $X$ is guaranteed to be non-negative ($X \ge 0$).</p>
     *
     * @return A pseudorandom {@code Double} value sampled from the Weibull distribution.
     */
    override fun sample(): Double {
        return distribution.sample()
    }

    /**
     * Contains constants and factory methods for {@code WeibullGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "weibull".
         */
        const val DISTRIBUTION_NAME = "weibull"

        /**
         * Factory method to create an instance of {@code WeibullGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} (shape, $k$): Defaults to **1.0**.</li>
         * <li>{@code param2} (scale, $\lambda$): Defaults to **1.0**.</li>
         * </ul>
         * <p>Using the defaults results in the **Exponential Distribution** with a rate $\lambda=1.0$,
         * as $\text{Weibull}(1, \lambda) \equiv \text{Exponential}(1/\lambda)$.</p>
         *
         * @param param1 The desired shape parameter ($k$) (can be {@code null}).
         * @param param2 The desired scale parameter ($\lambda$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance to be used by the underlying
         * Weibull distribution object.
         * @return A new {@code WeibullGenerator} instance.
         * @throws IllegalArgumentException if the effective {@code shape} or {@code scale} is not positive.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): WeibullGenerator {
            val shape = param1 ?: 1.0
            val scale = param2 ?: 1.0

            return WeibullGenerator(shape, scale, generator)
        }
    }
}