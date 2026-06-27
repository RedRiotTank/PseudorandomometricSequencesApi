package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Slash_distribution">Slash distribution</a>}.
 *
 * <p>Defined as the ratio of a standard Normal to an independent Uniform on $(0, 1)$:
 * $X = \mu + \sigma \cdot \mathcal{N}(0,1) / \mathcal{U}(0,1)$.
 * It has even heavier tails than the Cauchy distribution (no moments exist at all) and is
 * used as an outlier-robust alternative to the Normal in robust statistics and Bayesian
 * mixture models.</p>
 *
 * <p><strong>Algorithm</strong>:
 * <pre>
 *   Z ~ N(0, 1)
 *   U ~ Uniform(0, 1)
 *   return μ + σ · Z / U
 * </pre>
 * The division by $U \in (0, 1)$ inflates the tails: small values of $U$ produce extreme
 * observations, making the Slash arbitrarily heavy-tailed.</p>
 *
 * @param location The location parameter ($\mu$). Default 0.0.
 * @param scale The scale parameter ($\sigma$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Slash_distribution">Slash Distribution</a>
 */
class SlashGenerator(
    private val location: Double,
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Samples via the ratio definition: $X = \mu + \sigma \cdot Z / U$.
     */
    override fun sample(): Double {
        val z = randomGenerator.nextGaussian()
        val u = randomGenerator.nextDouble()
        return location + scale * z / u
    }

    companion object {
        const val DISTRIBUTION_NAME = "slash"

        /**
         * Default: $\mu = 0.0$, $\sigma = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): SlashGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            require(sigma > 0.0) { "Slash 'scale' (param2) must be strictly positive." }
            return SlashGenerator(mu, sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class SlashFactory : DistributionFactory {
    override val name = SlashGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0) — heavier tails than Cauchy"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        SlashGenerator.create(p1, p2, commonsRandom)
}
