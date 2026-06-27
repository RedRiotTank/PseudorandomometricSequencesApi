package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Chi_distribution">Chi distribution</a>}.
 *
 * <p>The distribution of the Euclidean norm of $k$ independent standard Normal variables.
 * Note: this is the Chi distribution, not Chi-squared. The Chi-squared distribution models
 * the <em>square</em> of this norm.</p>
 *
 * <p>Sampled as $X = \sqrt{Y}$ where $Y \sim \text{Gamma}(k/2,\; 2)$ (i.e. a Chi-squared
 * distribution with $k$ degrees of freedom).</p>
 *
 * @param degreesOfFreedom The degrees of freedom ($k$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Chi_distribution">Chi Distribution</a>
 */
class ChiGenerator(
    degreesOfFreedom: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val gamma: GammaDistribution = GammaDistribution(randomGenerator, degreesOfFreedom / 2.0, 2.0)

    override fun sample(): Double = sqrt(gamma.sample())

    companion object {
        const val DISTRIBUTION_NAME = "chi"

        /**
         * Default: $k = 1.0$ (degrees of freedom).
         */
        fun create(param1: Double?, generator: RandomGenerator): ChiGenerator {
            val k = param1 ?: 1.0
            require(k > 0.0) { "Chi 'degreesOfFreedom' (param1) must be strictly positive." }
            return ChiGenerator(k, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ChiFactory : DistributionFactory {
    override val name = ChiGenerator.DISTRIBUTION_NAME
    override val description = "param1 = degrees of freedom k (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ChiGenerator.create(p1, commonsRandom)
}
