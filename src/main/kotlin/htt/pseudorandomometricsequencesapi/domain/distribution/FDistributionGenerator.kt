package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.FDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following an
 * {@link <a href="https://en.wikipedia.org/wiki/F-distribution">F-distribution</a>}
 * (Fisher–Snedecor distribution).
 *
 * <p>Arises as the ratio of two scaled Chi-squared distributions. Used extensively in
 * ANOVA, regression analysis, and variance-ratio tests.</p>
 *
 * @param numeratorDf Numerator degrees of freedom ($d_1$). Must be strictly positive.
 * @param denominatorDf Denominator degrees of freedom ($d_2$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/F-distribution">F-Distribution</a>
 */
class FDistributionGenerator(
    numeratorDf: Double,
    denominatorDf: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: FDistribution = FDistribution(randomGenerator, numeratorDf, denominatorDf)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "f-distribution"

        /**
         * Default: $d_1 = 5.0$, $d_2 = 5.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): FDistributionGenerator {
            val d1 = param1 ?: 5.0
            val d2 = param2 ?: 5.0
            require(d1 > 0.0) { "F-distribution 'numeratorDf' (param1) must be strictly positive." }
            require(d2 > 0.0) { "F-distribution 'denominatorDf' (param2) must be strictly positive." }
            return FDistributionGenerator(d1, d2, generator)
        }
    }
}

@org.springframework.stereotype.Component
class FDistributionFactory : DistributionFactory {
    override val name = FDistributionGenerator.DISTRIBUTION_NAME
    override val description = "param1 = numerator df d1 (def. 5.0), param2 = denominator df d2 (def. 5.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        FDistributionGenerator.create(p1, p2, commonsRandom)
}
