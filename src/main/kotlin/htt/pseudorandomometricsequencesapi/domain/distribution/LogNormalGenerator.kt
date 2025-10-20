package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Log-normal_distribution">Lognormal distribution</a>}.
 *
 * <p>This class implements the {@code SequenceGenerator} interface and uses the
 * {@code org.apache.commons.math3.distribution.LogNormalDistribution} class from Apache
 * Commons Math for generating samples.</p>
 *
 * <p>A random variable $X$ is said to be log-normally distributed if its logarithm,
 * $\ln(X)$, is normally distributed. Since $\ln(X)$ is defined, $X$ must be a
 * positive random variable ($X > 0$).</p>
 *
 * <p>The distribution is parameterized by the mean ($\mu$) and standard deviation ($\sigma$)
 * of the **underlying normal distribution**, $\ln(X) \sim \mathcal{N}(\mu, \sigma^2)$.</p>
 *
 * <p>The probability density function (PDF) for $x > 0$ is:
 * $$ f(x; \mu, \sigma) = \frac{1}{x \sigma \sqrt{2\pi}} e^{-\frac{(\ln x - \mu)^2}{2\sigma^2}} $$</p>
 *
 * @param mu The <b>scale parameter</b> ($\mu$), which is the <b>mean of the logarithm</b>
 * of the variable, $\text{E}[\ln(X)]$. This is the first parameter of the underlying normal distribution.
 * @param sigma The <b>shape parameter</b> ($\sigma$), which is the <b>standard deviation
 * of the logarithm</b> of the variable, $\text{SD}[\ln(X)]$. Must be positive ($\sigma > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the Log-Normal distribution object for generating base pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Log-normal_distribution">Log-Normal Distribution</a>
 * @see org.apache.commons.math3.distribution.LogNormalDistribution
 */
class LogNormalGenerator(
    mu: Double,
    sigma: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * The internal Log-Normal distribution object from Apache Commons Math.
     */
    val distribution: LogNormalDistribution

    /**
     * Initializes the generator and validates the shape parameter.
     *
     * @throws IllegalArgumentException if {@code sigma} ($\sigma$) is not positive.
     */
    init {
        require(sigma > 0.0) { "LogNormal 'sigma' (param2) must be positive." }

        this.distribution = LogNormalDistribution(randomGenerator, mu, sigma)
    }

    /**
     * Generates and returns the next pseudorandom sample from the Log-Normal distribution.
     *
     * <p>The generated value $X$ is guaranteed to be positive ($X > 0$), as it is the exponentiated
     * result of a sample from the normal distribution, $X = e^Y$, where $Y \sim \mathcal{N}(\mu, \sigma^2)$.</p>
     *
     * @return A pseudorandom {@code Double} value sampled from the Log-Normal distribution.
     */
    override fun sample(): Double {
        return distribution.sample()
    }

    /**
     * Contains constants and factory methods for {@code LogNormalGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "lognormal".
         */
        const val DISTRIBUTION_NAME = "lognormal"

        /**
         * Factory method to create an instance of {@code LogNormalGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} ($\mu$, mean of log): Defaults to <b>0.0</b>.</li>
         * <li>{@code param2} ($\sigma$, standard deviation of log): Defaults to <b>1.0</b>.</li>
         * </ul>
         * <p>Using the defaults means the underlying normal distribution is the standard normal $\mathcal{N}(0, 1)$.
         * The resulting Log-Normal distribution $\text{Lognormal}(0, 1)$ has a mean of $e^{1/2} \approx 1.649$
         * and a mode of $e^{-1} \approx 0.368$.</p>
         *
         * @param param1 The desired mean of the logarithm ($\mu$) (can be {@code null}).
         * @param param2 The desired standard deviation of the logarithm ($\sigma$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance to be used by the generator.
         * @return A new {@code LogNormalGenerator} instance.
         * @throws IllegalArgumentException if the effective $\sigma$ is not positive.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LogNormalGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0

            return LogNormalGenerator(mu, sigma, generator)
        }
    }
}