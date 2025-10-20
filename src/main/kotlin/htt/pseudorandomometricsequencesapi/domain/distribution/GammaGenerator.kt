package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Gamma_distribution">Gamma distribution</a>}.
 *
 * <p>This class implements the {@code SequenceGenerator} interface and relies on the
 * {@code org.apache.commons.math3.distribution.GammaDistribution} class from Apache
 * Commons Math for its underlying sampling mechanism.</p>
 *
 * <p>The Gamma distribution is a two-parameter family of continuous probability distributions
 * defined over the positive real numbers ($x > 0$). It is highly versatile and includes the
 * <a href="https://en.wikipedia.org/wiki/Exponential_distribution">Exponential</a> and
 * <a href="https://en.wikipedia.org/wiki/Chi-squared_distribution">Chi-squared</a> distributions
 * as special cases.</p>
 *
 * <p>The distribution is parameterized by the <b>shape parameter</b> ($\alpha$ or $k$) and
 * the <b>scale parameter</b> ($\theta$ or $\beta$). The probability density function (PDF) is:
 * $$ f(x; \alpha, \theta) = \frac{x^{\alpha - 1} e^{-x/\theta}}{\theta^{\alpha} \Gamma(\alpha)} \quad \text{for } x > 0 $$
 * where $\Gamma(\alpha)$ is the Gamma function.</p>
 *
 * @param shape The <b>shape parameter</b> ($\alpha$) of the distribution. It dictates the
 * overall form and location of the peak. Must be positive ($\alpha > 0$).
 * @param scale The <b>scale parameter</b> ($\theta$) of the distribution. It influences the
 * distribution's spread. Must be positive ($\theta > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the Gamma distribution object for generating base pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Gamma_distribution">Gamma Distribution</a>
 * @see org.apache.commons.math3.distribution.GammaDistribution
 */
class GammaGenerator(
    shape: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * The internal Gamma distribution object from Apache Commons Math.
     */
    val distribution: GammaDistribution

    /**
     * Initializes the generator and validates the shape and scale parameters.
     *
     * @throws IllegalArgumentException if {@code shape} ($\alpha$) or {@code scale} ($\theta$)
     * are not positive.
     */
    init {
        require(shape > 0.0) { "Gamma 'shape' (param1) must be positive." }
        require(scale > 0.0) { "Gamma 'scale' (param2) must be positive." }

        this.distribution = GammaDistribution(randomGenerator, shape, scale)
    }

    /**
     * Generates and returns the next pseudorandom sample from the Gamma distribution
     * $\text{Gamma}(\text{shape}, \text{scale})$.
     *
     * <p>The generated value $X$ is non-negative ($X > 0$).</p>
     *
     * @return A pseudorandom {@code Double} value sampled from $\text{Gamma}(\alpha, \theta)$.
     */
    override fun sample(): Double {
        return distribution.sample()
    }

    /**
     * Contains constants and factory methods for {@code GammaGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "gamma".
         */
        const val DISTRIBUTION_NAME = "gamma"

        /**
         * Factory method to create an instance of {@code GammaGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} (shape, $\alpha$): Defaults to <b>1.0</b>.</li>
         * <li>{@code param2} (scale, $\theta$): Defaults to <b>1.0</b>.</li>
         * </ul>
         * <p>Using the defaults results in the <b>Standard Exponential Distribution</b>,
         * as $\text{Gamma}(1, 1)$ is equivalent to $\text{Exp}(1/\theta) = \text{Exp}(1)$.</p>
         *
         * @param param1 The desired shape parameter ($\alpha$) (can be {@code null}).
         * @param param2 The desired scale parameter ($\theta$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance to be used by the generator.
         * @return A new {@code GammaGenerator} instance.
         * @throws IllegalArgumentException if the effective {@code shape} or {@code scale} is not positive.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): GammaGenerator {
            val shape = param1 ?: 1.0
            val scale = param2 ?: 1.0


            return GammaGenerator(shape, scale, generator)
        }
    }
}