package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.LevyDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/L%C3%A9vy_distribution">Lévy distribution</a>}.
 *
 * <p>A stable distribution with index $\alpha = 1/2$, exhibiting very heavy tails ($\infty$ variance).
 * Used in financial modelling (Lévy flights), anomalous diffusion, and first-passage time problems.</p>
 *
 * @param location The location (shift) parameter ($\mu$).
 * @param scale The scale parameter ($c$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/L%C3%A9vy_distribution">Lévy Distribution</a>
 */
class LevyGenerator(
    location: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: LevyDistribution = LevyDistribution(randomGenerator, location, scale)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "levy"

        /**
         * Default: $\mu = 0.0$, $c = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LevyGenerator {
            val mu = param1 ?: 0.0
            val c = param2 ?: 1.0
            require(c > 0.0) { "Lévy 'scale' (param2) must be strictly positive." }
            return LevyGenerator(mu, c, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LevyFactory : DistributionFactory {
    override val name = LevyGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale c (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LevyGenerator.create(p1, p2, commonsRandom)
}
