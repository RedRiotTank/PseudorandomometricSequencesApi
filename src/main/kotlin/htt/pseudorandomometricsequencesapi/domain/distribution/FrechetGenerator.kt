package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Fr%C3%A9chet_distribution">Fréchet distribution</a>}
 * (Extreme Value Type II).
 *
 * <p>Exhibits very heavy tails and is used to model extreme events with no finite upper bound,
 * such as large insurance claims, earthquake magnitudes, and maximum flood levels.</p>
 *
 * <p>Sampling uses the exact inverse-CDF: $X = \mu + \sigma \cdot (-\ln U)^{-1/\alpha}$.</p>
 *
 * @param shape The shape parameter ($\alpha$). Must be strictly positive.
 * @param scale The scale parameter ($\sigma$). Must be strictly positive.
 * @param location The location (shift) parameter ($\mu$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Fr%C3%A9chet_distribution">Fréchet Distribution</a>
 */
class FrechetGenerator(
    private val shape: Double,
    private val scale: Double,
    private val location: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = \mu + \sigma \cdot (-\ln U)^{-1/\alpha}$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return location + scale * (-ln(u)).pow(-1.0 / shape)
    }

    companion object {
        const val DISTRIBUTION_NAME = "frechet"

        /**
         * Default: $\alpha = 1.0$ (shape), $\sigma = 1.0$ (scale), $\mu = 0.0$ (location).
         */
        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): FrechetGenerator {
            val alpha = param1 ?: 1.0
            val sigma = param2 ?: 1.0
            val mu = param3 ?: 0.0
            require(alpha > 0.0) { "Fréchet 'shape' (param1) must be strictly positive." }
            require(sigma > 0.0) { "Fréchet 'scale' (param2) must be strictly positive." }
            return FrechetGenerator(alpha, sigma, mu, generator)
        }
    }
}

@org.springframework.stereotype.Component
class FrechetFactory : DistributionFactory {
    override val name = FrechetGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape α (def. 1.0), param2 = scale σ (def. 1.0), param3 = location μ (def. 0.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        FrechetGenerator.create(p1, p2, p3, commonsRandom)
}
