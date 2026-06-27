package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Lomax_distribution">Lomax distribution</a>}
 * (Pareto Type II).
 *
 * <p>A heavy-tailed distribution that can be thought of as a Pareto distribution shifted so that
 * its support starts at zero. Used in reliability engineering, queuing theory, and internet
 * traffic modelling.</p>
 *
 * <p>Sampling uses the exact inverse-CDF: $X = \lambda \left((1-U)^{-1/\alpha} - 1\right)$.</p>
 *
 * @param shape The shape parameter ($\alpha$). Must be strictly positive.
 * @param scale The scale parameter ($\lambda$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Lomax_distribution">Lomax Distribution</a>
 */
class LomaxGenerator(
    private val shape: Double,
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = \lambda ((1 - U)^{-1/\alpha} - 1)$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return scale * ((1.0 - u).pow(-1.0 / shape) - 1.0)
    }

    companion object {
        const val DISTRIBUTION_NAME = "lomax"

        /**
         * Default: $\alpha = 1.0$ (shape), $\lambda = 1.0$ (scale).
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LomaxGenerator {
            val alpha = param1 ?: 1.0
            val lambda = param2 ?: 1.0
            require(alpha > 0.0) { "Lomax 'shape' (param1) must be strictly positive." }
            require(lambda > 0.0) { "Lomax 'scale' (param2) must be strictly positive." }
            return LomaxGenerator(alpha, lambda, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LomaxFactory : DistributionFactory {
    override val name = LomaxGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape α (def. 1.0), param2 = scale λ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LomaxGenerator.create(p1, p2, commonsRandom)
}
