package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.WeibullDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Double_Weibull_distribution">Double Weibull distribution</a>}.
 *
 * <p>The distribution of $\text{sign}(V-0.5) \cdot W$ where $V \sim \mathcal{U}(0,1)$ and
 * $W \sim \text{Weibull}(k, \lambda)$. Symmetric around zero with values on $(-\infty, \infty)$.
 * Used when modelling phenomena that may deviate positively or negatively from zero with
 * Weibull-shaped tails (wind speed components, financial returns).</p>
 *
 * @param shape Shape parameter ($k > 0$). Default 2.0.
 * @param scale Scale parameter ($\lambda > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Double_Weibull_distribution">Double Weibull Distribution</a>
 */
class DoubleWeibullGenerator(
    shape: Double,
    scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val weibull = WeibullDistribution(randomGenerator, shape, scale)

    override fun sample(): Double {
        val x = weibull.sample()
        return if (randomGenerator.nextDouble() < 0.5) x else -x
    }

    companion object {
        const val DISTRIBUTION_NAME = "double-weibull"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): DoubleWeibullGenerator {
            val k      = param1 ?: 2.0
            val lambda = param2 ?: 1.0
            require(k      > 0.0) { "Double Weibull 'shape' (param1) must be strictly positive." }
            require(lambda > 0.0) { "Double Weibull 'scale' (param2) must be strictly positive." }
            return DoubleWeibullGenerator(k, lambda, generator)
        }
    }
}

@org.springframework.stereotype.Component
class DoubleWeibullFactory : DistributionFactory {
    override val name = DoubleWeibullGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape k (def. 2.0), param2 = scale λ (def. 1.0) — symmetric Weibull around 0"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        DoubleWeibullGenerator.create(p1, p2, commonsRandom)
}
