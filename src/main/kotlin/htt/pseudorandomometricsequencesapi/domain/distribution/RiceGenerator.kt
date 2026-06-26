package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Rice_distribution">Rice (Rician) distribution</a>}.
 *
 * <p>Models the envelope of a sinusoidal signal in the presence of Gaussian noise. Commonly used
 * in wireless communications (received signal strength), MRI magnitude imaging, and radar signal
 * processing. When $\nu = 0$ it reduces to a Rayleigh distribution.</p>
 *
 * <p>Sampled as $X = \sqrt{(\mathcal{N}(\nu, \sigma))^2 + (\mathcal{N}(0, \sigma))^2}$, which is
 * the exact definition of the Rice distribution.</p>
 *
 * @param nu The non-centrality parameter ($\nu \ge 0$).
 * @param sigma The noise standard deviation ($\sigma \gt 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Rice_distribution">Rice Distribution</a>
 */
class RiceGenerator(
    private val nu: Double,
    private val sigma: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val x = randomGenerator.nextGaussian() * sigma + nu
        val y = randomGenerator.nextGaussian() * sigma
        return sqrt(x * x + y * y)
    }

    companion object {
        const val DISTRIBUTION_NAME = "rice"

        /**
         * Default: $\nu = 0.0$ (Rayleigh), $\sigma = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): RiceGenerator {
            val nu = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            require(nu >= 0.0) { "Rice 'nu' (param1) must be non-negative." }
            require(sigma > 0.0) { "Rice 'sigma' (param2) must be strictly positive." }
            return RiceGenerator(nu, sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class RiceFactory : DistributionFactory {
    override val name = RiceGenerator.DISTRIBUTION_NAME
    override val description = "param1 = non-centrality ν ≥ 0 (def. 0.0), param2 = scale σ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        RiceGenerator.create(p1, p2, commonsRandom)
}
