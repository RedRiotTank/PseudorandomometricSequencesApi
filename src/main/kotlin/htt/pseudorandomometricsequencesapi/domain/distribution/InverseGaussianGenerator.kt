package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following an
 * {@link <a href="https://en.wikipedia.org/wiki/Inverse_Gaussian_distribution">Inverse Gaussian distribution</a>}
 * (also known as the Wald distribution).
 *
 * <p>Models the first-passage time of Brownian motion with positive drift. Widely used in
 * reliability engineering, sequential analysis, and neuroscience (reaction-time modelling).</p>
 *
 * <p>Sampling uses the Michael–Schucany–Haas (1976) algorithm, which is exact and efficient.</p>
 *
 * @param mean The mean ($\mu$). Must be strictly positive.
 * @param shape The shape parameter ($\lambda$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Inverse_Gaussian_distribution">Inverse Gaussian Distribution</a>
 */
class InverseGaussianGenerator(
    private val mean: Double,
    private val shape: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Michael–Schucany–Haas (1976) exact sampler.
     */
    override fun sample(): Double {
        val y = randomGenerator.nextGaussian()
        val y2 = y * y
        val x = mean + (mean * mean * y2) / (2.0 * shape) -
                (mean / (2.0 * shape)) * sqrt(4.0 * mean * shape * y2 + mean * mean * y2 * y2)
        val u = randomGenerator.nextDouble()
        return if (u <= mean / (mean + x)) x else (mean * mean) / x
    }

    companion object {
        const val DISTRIBUTION_NAME = "inverse-gaussian"

        /**
         * Default: $\mu = 1.0$, $\lambda = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): InverseGaussianGenerator {
            val mu = param1 ?: 1.0
            val lambda = param2 ?: 1.0
            require(mu > 0.0) { "Inverse Gaussian 'mean' (param1) must be strictly positive." }
            require(lambda > 0.0) { "Inverse Gaussian 'shape' (param2) must be strictly positive." }
            return InverseGaussianGenerator(mu, lambda, generator)
        }
    }
}

@org.springframework.stereotype.Component
class InverseGaussianFactory : DistributionFactory {
    override val name = InverseGaussianGenerator.DISTRIBUTION_NAME
    override val description = "param1 = mean μ (def. 1.0), param2 = shape λ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        InverseGaussianGenerator.create(p1, p2, commonsRandom)
}
