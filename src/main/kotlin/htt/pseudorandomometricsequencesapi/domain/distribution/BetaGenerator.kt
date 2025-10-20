package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Beta_distribution">Beta distribution</a>}.
 *
 * <p>This class implements the {@code SequenceGenerator} interface and relies on the
 * {@code org.apache.commons.math3.distribution.BetaDistribution} class for its
 * sampling logic.</p>
 *
 * <p>The Beta distribution is a continuous probability distribution defined on the interval
 * $[0, 1]$, and is parameterized by two positive shape parameters, $\alpha$ and $\beta$.
 * It is often used as a prior distribution in Bayesian statistics, particularly for
 * proportions and probabilities.</p>
 *
 * <p>The probability density function (PDF) is given by:
 * $$ f(x; \alpha, \beta) = \frac{x^{\alpha - 1} (1 - x)^{\beta - 1}}{B(\alpha, \beta)} $$
 * where $B(\alpha, \beta)$ is the Beta function.</p>
 *
 * @param alpha The first <b>shape parameter</b> ($\alpha$), often referred to as the
 * "successes" parameter. Must be positive ($\alpha > 0$).
 * @param beta The second <b>shape parameter</b> ($\beta$), often referred to as the
 * "failures" parameter. Must be positive ($\beta > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}
 * instance used by the Beta distribution for generating base uniform pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Beta_distribution">Beta Distribution</a>
 * @see org.apache.commons.math3.distribution.BetaDistribution
 */
class BetaGenerator(
    alpha: Double,
    beta: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * The internal Beta distribution object from Apache Commons Math.
     */
    val distribution: BetaDistribution

    /**
     * Initializes the generator and validates the shape parameters.
     *
     * @throws IllegalArgumentException if {@code alpha} or {@code beta} are not positive
     * ($\alpha \le 0$ or $\beta \le 0$).
     */
    init {
        require(alpha > 0.0) { "Beta 'alpha' (param1) must be positive." }
        require(beta > 0.0) { "Beta 'beta' (param2) must be positive." }

        this.distribution = BetaDistribution(randomGenerator, alpha, beta)
    }

    /**
     * Generates and returns the next pseudorandom sample from the Beta distribution
     * $\text{Beta}(\alpha, \beta)$.
     *
     * <p>The generated value $X$ is guaranteed to be within the unit interval $[0, 1]$.</p>
     *
     * @return A pseudorandom {@code Double} value sampled from $\text{Beta}(\alpha, \beta)$.
     */
    override fun sample(): Double {
        return distribution.sample()
    }

    /**
     * Contains constants and factory methods for {@code BetaGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "beta".
         */
        const val DISTRIBUTION_NAME = "beta"

        /**
         * Factory method to create an instance of {@code BetaGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} (alpha, $\alpha$): Defaults to <b>1.0</b>.</li>
         * <li>{@code param2} (beta, $\beta$): Defaults to <b>1.0</b>.</li>
         * </ul>
         * <p>Using the defaults results in the <b>Continuous Uniform Distribution</b> on $[0, 1]$,
         * as $\text{Beta}(1, 1)$ simplifies to a uniform distribution.</p>
         *
         * @param param1 The desired alpha shape parameter ($\alpha$) (can be {@code null}).
         * @param param2 The desired beta shape parameter ($\beta$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance to be used by the underlying
         * Beta distribution object.
         * @return A new {@code BetaGenerator} instance.
         * @throws IllegalArgumentException if the effective {@code alpha} or {@code beta} is not positive.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): BetaGenerator {
            val alpha = param1 ?: 1.0
            val beta = param2 ?: 1.0

            return BetaGenerator(alpha, beta, generator)
        }
    }
}