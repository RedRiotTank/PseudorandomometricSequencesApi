package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Moyal_distribution">Moyal distribution</a>}.
 *
 * <p>A closed-form approximation to the Landau distribution that retains the key features
 * (asymmetric, heavy right tail) while admitting an exact inverse-CDF. Originally introduced
 * by J. E. Moyal (1955). Used as a fast substitute for Landau in Monte Carlo codes where
 * exact Landau sampling is expensive.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   X = μ − 2σ · ln(Φ⁻¹(1 − U/2))
 * </pre>
 * where $\Phi^{-1}$ is the standard Normal quantile function.
 * Mean $\approx \mu + \sigma(\gamma_E + \ln 2) \approx \mu + 1.2704\sigma$.</p>
 *
 * @param location Location parameter ($\mu$). Default 0.0.
 * @param scale Scale parameter ($\sigma > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Moyal_distribution">Moyal Distribution</a>
 */
class MoyalGenerator(
    private val location: Double,
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val normal = NormalDistribution(randomGenerator, 0.0, 1.0)

    /** Exact inverse-CDF: $X = \mu - 2\sigma \ln(\Phi^{-1}(1 - U/2))$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble().coerceIn(1e-15, 1.0 - 1e-15)
        val q = normal.inverseCumulativeProbability(1.0 - u / 2.0)
        return location - 2.0 * scale * ln(q)
    }

    companion object {
        const val DISTRIBUTION_NAME = "moyal"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): MoyalGenerator {
            val mu    = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            require(sigma > 0.0) { "Moyal 'scale' (param2) must be strictly positive." }
            return MoyalGenerator(mu, sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class MoyalFactory : DistributionFactory {
    override val name = MoyalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0) — closed-form Landau approximation"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        MoyalGenerator.create(p1, p2, commonsRandom)
}
