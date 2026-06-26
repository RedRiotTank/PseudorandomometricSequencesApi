package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.abs

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Half-normal_distribution">Half-Normal distribution</a>}.
 *
 * <p>The distribution of the absolute value of a zero-mean Normal random variable: $X = |\mathcal{N}(0, \sigma)|$.
 * Produces only non-negative values. Widely used as a prior for scale parameters in Bayesian
 * hierarchical models.</p>
 *
 * @param scale The scale parameter ($\sigma$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Half-normal_distribution">Half-Normal Distribution</a>
 */
class HalfNormalGenerator(
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val normal: NormalDistribution = NormalDistribution(randomGenerator, 0.0, scale)

    override fun sample(): Double = abs(normal.sample())

    companion object {
        const val DISTRIBUTION_NAME = "half-normal"

        /**
         * Default: $\sigma = 1.0$.
         */
        fun create(param1: Double?, generator: RandomGenerator): HalfNormalGenerator {
            val sigma = param1 ?: 1.0
            require(sigma > 0.0) { "Half-Normal 'scale' (param1) must be strictly positive." }
            return HalfNormalGenerator(sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class HalfNormalFactory : DistributionFactory {
    override val name = HalfNormalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = scale σ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        HalfNormalGenerator.create(p1, commonsRandom)
}
