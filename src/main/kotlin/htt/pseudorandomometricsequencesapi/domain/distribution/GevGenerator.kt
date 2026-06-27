package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Generalized_extreme_value_distribution">Generalized Extreme Value (GEV) distribution</a>}.
 *
 * <p>Unifies the three extreme-value families via the shape parameter $\xi$:</p>
 * <ul>
 *   <li>$\xi = 0$: Gumbel (Type I)</li>
 *   <li>$\xi \gt 0$: Fréchet (Type II) — heavy tail</li>
 *   <li>$\xi \lt 0$: Weibull-type (Type III) — bounded upper tail</li>
 * </ul>
 * <p>Sampling uses the exact inverse-CDF method.</p>
 *
 * @param location The location parameter ($\mu$).
 * @param scale The scale parameter ($\sigma$). Must be strictly positive.
 * @param shape The shape parameter ($\xi$). Use 0.0 for Gumbel.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Generalized_extreme_value_distribution">GEV Distribution</a>
 */
class GevGenerator(
    private val location: Double,
    private val scale: Double,
    private val shape: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        val t = -ln(u)
        return if (Math.abs(shape) < 1e-10) {
            location - scale * ln(t)
        } else {
            location + scale * (t.pow(-shape) - 1.0) / shape
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "gev"

        /**
         * Default: $\mu = 0.0$, $\sigma = 1.0$, $\xi = 0.0$ (Gumbel).
         */
        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): GevGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            val xi = param3 ?: 0.0
            require(sigma > 0.0) { "GEV 'scale' (param2) must be strictly positive." }
            return GevGenerator(mu, sigma, xi, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GevFactory : DistributionFactory {
    override val name = GevGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0), param3 = shape ξ (def. 0.0; 0=Gumbel, >0=Fréchet-type, <0=bounded)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GevGenerator.create(p1, p2, p3, commonsRandom)
}
