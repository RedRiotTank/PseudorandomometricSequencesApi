package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Landau_distribution">Landau distribution</a>}.
 *
 * <p>Describes the stochastic energy loss of a fast charged particle traversing a thin layer
 * of matter (Bethe–Bloch fluctuations). Asymmetric with a characteristic long right tail
 * (occasional large energy transfers to electrons), widely used in detector simulation
 * (GEANT4, ROOT/CERN, FLUKA).</p>
 *
 * <p><strong>Algorithm</strong> — Chambers–Mallows–Stuck (1976) exact sampler for the
 * $\alpha=1$, $\beta=1$ Lévy-stable distribution that corresponds to the Landau:
 * <pre>
 *   φ = π·(U₁ − 1/2)
 *   W = −ln(U₂)
 *   Z = (2/π)·[(π/2 + φ)·tan(φ) − ln(π·W·cos(φ)/(π/2 + φ))]
 *   X = μ + σ·Z
 * </pre></p>
 *
 * @param location Location (most-probable value shift, $\mu$). Default 0.0.
 * @param scale Scale parameter ($\sigma > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Landau_distribution">Landau Distribution</a>
 */
class LandauGenerator(
    private val location: Double,
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u1 = randomGenerator.nextDouble().coerceIn(1e-15, 1.0 - 1e-15)
        val u2 = randomGenerator.nextDouble().coerceAtLeast(1e-15)
        val phi = PI * (u1 - 0.5)
        val w   = -ln(u2)
        val halfPiPlusPhi = PI / 2.0 + phi
        val z = (2.0 / PI) * (halfPiPlusPhi * tan(phi) - ln(PI * w * cos(phi) / halfPiPlusPhi))
        return location + scale * z
    }

    companion object {
        const val DISTRIBUTION_NAME = "landau"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LandauGenerator {
            val mu    = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            require(sigma > 0.0) { "Landau 'scale' (param2) must be strictly positive." }
            return LandauGenerator(mu, sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LandauFactory : DistributionFactory {
    override val name = LandauGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0) — asymmetric; no finite mean"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LandauGenerator.create(p1, p2, commonsRandom)
}
