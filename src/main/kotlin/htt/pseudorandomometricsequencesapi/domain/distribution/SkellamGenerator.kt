package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.PoissonDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Skellam_distribution">Skellam distribution</a>}.
 *
 * <p>The distribution of the difference between two independent Poisson random variables:
 * $X = Y_1 - Y_2$ where $Y_1 \sim \text{Poisson}(\mu_1)$ and $Y_2 \sim \text{Poisson}(\mu_2)$.
 * Takes integer values on $\mathbb{Z}$. Applied in modelling score differences in football/basketball
 * and activity differences in biology (e.g., photon counts).</p>
 *
 * <p>Mean $= \mu_1 - \mu_2$, Variance $= \mu_1 + \mu_2$.</p>
 *
 * @param mu1 Rate of the first Poisson ($\mu_1 > 0$). Default 1.0.
 * @param mu2 Rate of the second Poisson ($\mu_2 > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Skellam_distribution">Skellam Distribution</a>
 */
class SkellamGenerator(
    mu1: Double,
    mu2: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val poisson1: PoissonDistribution = PoissonDistribution(
        randomGenerator, mu1,
        PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS
    )
    private val poisson2: PoissonDistribution = PoissonDistribution(
        randomGenerator, mu2,
        PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS
    )

    override fun sample(): Double = (poisson1.sample() - poisson2.sample()).toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "skellam"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): SkellamGenerator {
            val mu1 = param1 ?: 1.0
            val mu2 = param2 ?: 1.0
            require(mu1 > 0.0) { "Skellam 'mu1' (param1) must be strictly positive." }
            require(mu2 > 0.0) { "Skellam 'mu2' (param2) must be strictly positive." }
            return SkellamGenerator(mu1, mu2, generator)
        }
    }
}

@org.springframework.stereotype.Component
class SkellamFactory : DistributionFactory {
    override val name = SkellamGenerator.DISTRIBUTION_NAME
    override val description = "param1 = Poisson rate μ₁ (def. 1.0), param2 = Poisson rate μ₂ (def. 1.0) — difference of two Poissons"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        SkellamGenerator.create(p1, p2, commonsRandom)
}
