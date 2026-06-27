package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Hyperbolic_secant_distribution">Hyperbolic Secant distribution</a>}.
 *
 * <p>Symmetric bell-shaped distribution on $\mathbb{R}$ with PDF
 * $f(x) = \frac{1}{2}\,\text{sech}\!\left(\frac{\pi x}{2}\right)$. Has heavier tails than the
 * Normal but lighter than the Logistic. The squared hyperbolic secant function is its own
 * characteristic function.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   X = μ + σ · (2/π) · ln(tan(π·U/2))
 * </pre>
 * Mean $= \mu$, Variance $= \sigma^2$.</p>
 *
 * @param location Location parameter ($\mu$). Default 0.0.
 * @param scale Scale parameter ($\sigma > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Hyperbolic_secant_distribution">Hyperbolic Secant Distribution</a>
 */
class HyperbolicSecantGenerator(
    private val location: Double,
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = \mu + \sigma \cdot (2/\pi) \cdot \ln(\tan(\pi U/2))$. */
    override fun sample(): Double {
        // Clamp away from 0 and 1 to avoid ln(0) = -∞
        val u = randomGenerator.nextDouble().coerceIn(1e-15, 1.0 - 1e-15)
        return location + scale * (2.0 / PI) * ln(tan(PI * u / 2.0))
    }

    companion object {
        const val DISTRIBUTION_NAME = "hyperbolic-secant"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): HyperbolicSecantGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            require(sigma > 0.0) { "Hyperbolic Secant 'scale' (param2) must be strictly positive." }
            return HyperbolicSecantGenerator(mu, sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class HyperbolicSecantFactory : DistributionFactory {
    override val name = HyperbolicSecantGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        HyperbolicSecantGenerator.create(p1, p2, commonsRandom)
}
