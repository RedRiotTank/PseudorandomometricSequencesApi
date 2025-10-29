package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.CauchyDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Cauchy_distribution">Cauchy distribution</a>} (also known as the Lorentzian distribution).
 *
 * <p>This distribution is notable for its heavy tails, which means it generates extreme
 * values with much higher probability than the Normal distribution. It does not have
 * a defined mean or variance.</p>
 *
 * <p>The probability density function (PDF) is:
 * $$ f(x; x_0, \gamma) = \frac{1}{\pi \gamma \left[1 + \left(\frac{x - x_0}{\gamma}\right)^2\right]} $$
 * where $x_0$ is the location parameter and $\gamma$ is the scale parameter.</p>
 *
 * @param location The **location parameter** ($x_0$). This value represents the mode and median
 * of the distribution.
 * @param scale The **scale parameter** ($\gamma$). This value determines the half-width at half-maximum (HWHM)
 * of the distribution's peak. Must be positive ($\gamma > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the Cauchy distribution for generating base pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cauchy_distribution">Cauchy Distribution</a>
 * @see org.apache.commons.math3.distribution.CauchyDistribution
 */
class CauchyGenerator(
    val location: Double,
    val scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * The internal Cauchy distribution object from Apache Commons Math.
     * The initialization and validation are performed here to ensure 100% coverage
     * of the property declaration line.
     */
    val distribution: CauchyDistribution = run {
        require(scale > 0.0) { "Cauchy 'scale' (param2) must be positive (gamma > 0)." }
        CauchyDistribution(randomGenerator, location, scale)
    }

    /**
     * Generates and returns the next pseudorandom sample from the Cauchy distribution.
     *
     * <p>The generated value $X$ can be any real number ($-\infty < X < \infty$).</p>
     *
     * @return A pseudorandom {@code Double} value sampled from the Cauchy distribution.
     */
    override fun sample(): Double {
        return distribution.sample()
    }

    /**
     * Contains constants and factory methods for {@code CauchyGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "cauchy".
         */
        const val DISTRIBUTION_NAME = "cauchy"

        /**
         * Factory method to create an instance of {@code CauchyGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} (location, $x_0$): Defaults to **0.0**.</li>
         * <li>{@code param2} (scale, $\gamma$): Defaults to **1.0**.</li>
         * </ul>
         * <p>Using the defaults results in the **Standard Cauchy Distribution**, $\text{Cauchy}(0, 1)$.</p>
         *
         * @param param1 The desired location parameter ($x_0$) (can be {@code null}).
         * @param param2 The desired scale parameter ($\gamma$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance to be used by the underlying
         * Cauchy distribution object.
         * @return A new {@code CauchyGenerator} instance.
         * @throws IllegalArgumentException if the effective {@code scale} is not positive.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): CauchyGenerator {
            val location = param1 ?: 0.0
            val scale = param2 ?: 1.0

            return CauchyGenerator(location, scale, generator)
        }
    }
}