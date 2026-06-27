package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.abs

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Folded_normal_distribution">Folded Normal distribution</a>}.
 *
 * <p>The distribution of $|X|$ where $X \sim \mathcal{N}(\mu, \sigma^2)$. Unlike the Half-Normal
 * (which fixes $\mu = 0$), the Folded Normal allows $\mu \ne 0$, producing an asymmetric
 * distribution on $[0, \infty)$. Used in quality control (absolute deviations from a target),
 * radar signal modelling, and Bayesian hierarchical models.</p>
 *
 * <p><strong>Algorithm</strong>: $X = |\mathcal{N}(\mu, \sigma^2)|$.</p>
 *
 * @param mu Mean of the underlying Normal. Default 0.0.
 * @param sigma Std. deviation ($\sigma > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Folded_normal_distribution">Folded Normal Distribution</a>
 */
class FoldedNormalGenerator(
    mu: Double,
    sigma: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val normal = NormalDistribution(randomGenerator, mu, sigma)

    override fun sample(): Double = abs(normal.sample())

    companion object {
        const val DISTRIBUTION_NAME = "folded-normal"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): FoldedNormalGenerator {
            val mu    = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            require(sigma > 0.0) { "Folded Normal 'sigma' (param2) must be strictly positive." }
            return FoldedNormalGenerator(mu, sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class FoldedNormalFactory : DistributionFactory {
    override val name = FoldedNormalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = mean μ (def. 0.0), param2 = std. deviation σ (def. 1.0) — absolute value of Normal"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        FoldedNormalGenerator.create(p1, p2, commonsRandom)
}
