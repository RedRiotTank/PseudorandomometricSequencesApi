package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Rayleigh_distribution">Rayleigh distribution</a>}.
 *
 * <p>A special case of the Weibull distribution (shape $k = 2$). Arises when modelling the
 * magnitude of a 2D vector whose components are independent zero-mean Gaussians. Common in
 * wireless communications (signal envelope), wind speed analysis, and wave height modelling.</p>
 *
 * <p>Sampled via the exact inverse-CDF method: $X = \sigma \sqrt{-2 \ln U}$, where $U \sim \text{Uniform}(0,1)$.</p>
 *
 * @param scale The scale parameter ($\sigma$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Rayleigh_distribution">Rayleigh Distribution</a>
 */
class RayleighGenerator(
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = \sigma \sqrt{-2 \ln U}$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return scale * sqrt(-2.0 * ln(u))
    }

    companion object {
        const val DISTRIBUTION_NAME = "rayleigh"

        /**
         * Default: $\sigma = 1.0$.
         */
        fun create(param1: Double?, generator: RandomGenerator): RayleighGenerator {
            val sigma = param1 ?: 1.0
            require(sigma > 0.0) { "Rayleigh 'scale' (param1) must be strictly positive." }
            return RayleighGenerator(sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class RayleighFactory : DistributionFactory {
    override val name = RayleighGenerator.DISTRIBUTION_NAME
    override val description = "param1 = scale σ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        RayleighGenerator.create(p1, commonsRandom)
}
