package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GumbelDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Gumbel_distribution">Gumbel distribution</a>}
 * (Extreme Value Type I).
 *
 * <p>Used to model the distribution of the maximum (or minimum) of a number of samples from
 * various distributions. Common applications include extreme meteorological events, flood
 * frequency analysis, and financial risk modelling.</p>
 *
 * @param location The location parameter ($\mu$).
 * @param scale The scale parameter ($\beta$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Gumbel_distribution">Gumbel Distribution</a>
 */
class GumbelGenerator(
    location: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: GumbelDistribution = GumbelDistribution(randomGenerator, location, scale)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "gumbel"

        /**
         * Default: $\mu = 0.0$, $\beta = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): GumbelGenerator {
            val mu = param1 ?: 0.0
            val beta = param2 ?: 1.0
            require(beta > 0.0) { "Gumbel 'scale' (param2) must be strictly positive." }
            return GumbelGenerator(mu, beta, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GumbelFactory : DistributionFactory {
    override val name = GumbelGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale β (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GumbelGenerator.create(p1, p2, commonsRandom)
}
