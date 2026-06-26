package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Truncated_normal_distribution">Truncated Normal distribution</a>}.
 *
 * <p>A Normal distribution restricted to the interval $[\mu - w\sigma,\; \mu + w\sigma]$, where
 * $w$ is the half-width expressed in standard deviations. Sampling uses the exact inverse-CDF
 * method via the underlying {@code NormalDistribution}.</p>
 *
 * @param mu The mean of the underlying Normal ($\mu$).
 * @param sigma The standard deviation of the underlying Normal ($\sigma$). Must be strictly positive.
 * @param halfWidthSigmas Half-width of the truncation window in units of $\sigma$ ($w > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Truncated_normal_distribution">Truncated Normal Distribution</a>
 */
class TruncatedNormalGenerator(
    private val mu: Double,
    private val sigma: Double,
    private val halfWidthSigmas: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val normal = NormalDistribution(randomGenerator, mu, sigma)
    private val pLower: Double = normal.cumulativeProbability(mu - halfWidthSigmas * sigma)
    private val pUpper: Double = normal.cumulativeProbability(mu + halfWidthSigmas * sigma)
    private val pRange: Double = pUpper - pLower

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return normal.inverseCumulativeProbability(pLower + u * pRange)
    }

    companion object {
        const val DISTRIBUTION_NAME = "truncated-normal"

        /**
         * Default: $\mu = 0.0$, $\sigma = 1.0$, $w = 3.0$ (truncates at $\pm 3\sigma$).
         */
        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): TruncatedNormalGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            val w = param3 ?: 3.0
            require(sigma > 0.0) { "Truncated Normal 'sigma' (param2) must be strictly positive." }
            require(w > 0.0) { "Truncated Normal 'halfWidthSigmas' (param3) must be strictly positive." }
            return TruncatedNormalGenerator(mu, sigma, w, generator)
        }
    }
}

@org.springframework.stereotype.Component
class TruncatedNormalFactory : DistributionFactory {
    override val name = TruncatedNormalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = mean μ (def. 0.0), param2 = std. deviation σ (def. 1.0), param3 = half-width in σ units w (def. 3.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        TruncatedNormalGenerator.create(p1, p2, p3, commonsRandom)
}
