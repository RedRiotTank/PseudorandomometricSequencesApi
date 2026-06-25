package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.LaplaceDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Laplace_distribution">Laplace distribution</a>}.
 *
 * <p>Also known as the Double Exponential distribution. It is symmetric around the location
 * parameter $\mu$ with heavier tails than the Gaussian, making it robust to outliers and
 * widely used in Bayesian statistics and machine learning (L1 regularisation prior).</p>
 *
 * @param location The location parameter ($\mu$).
 * @param scale The scale parameter ($b$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Laplace_distribution">Laplace Distribution</a>
 */
class LaplaceGenerator(
    location: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: LaplaceDistribution = LaplaceDistribution(randomGenerator, location, scale)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "laplace"

        /**
         * Default: $\mu = 0.0$, $b = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LaplaceGenerator {
            val mu = param1 ?: 0.0
            val b = param2 ?: 1.0
            require(b > 0.0) { "Laplace 'scale' (param2) must be strictly positive." }
            return LaplaceGenerator(mu, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LaplaceFactory : DistributionFactory {
    override val name = LaplaceGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale b (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LaplaceGenerator.create(p1, p2, commonsRandom)
}
