package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.LogisticDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Logistic_distribution">Logistic distribution</a>}.
 *
 * <p>This continuous distribution has a symmetric bell-shaped PDF similar to the Normal but with
 * heavier tails. Its CDF is the logistic (sigmoid) function. Widely used as the basis of logistic
 * regression and S-curve modelling.</p>
 *
 * @param location The location parameter ($\mu$), equivalent to the mean and median.
 * @param scale The scale parameter ($s$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Logistic_distribution">Logistic Distribution</a>
 */
class LogisticGenerator(
    location: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: LogisticDistribution = LogisticDistribution(randomGenerator, location, scale)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "logistic"

        /**
         * Default: $\mu = 0.0$, $s = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LogisticGenerator {
            val mu = param1 ?: 0.0
            val s = param2 ?: 1.0
            require(s > 0.0) { "Logistic 'scale' (param2) must be strictly positive." }
            return LogisticGenerator(mu, s, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LogisticFactory : DistributionFactory {
    override val name = LogisticGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale s (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LogisticGenerator.create(p1, p2, commonsRandom)
}
