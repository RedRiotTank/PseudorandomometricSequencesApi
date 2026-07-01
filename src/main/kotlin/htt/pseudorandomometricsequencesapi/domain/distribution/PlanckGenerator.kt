package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for photon energies following the
 * {@link <a href="https://en.wikipedia.org/wiki/Planck%27s_law">Planck (blackbody) distribution</a>}.
 *
 * <p>Describes the spectral energy distribution of electromagnetic radiation in thermal
 * equilibrium at temperature $T$. Fundamental in cosmology (CMB photons), stellar physics,
 * laser physics, and thermal detector simulation.</p>
 *
 * <p>PDF: $f(\varepsilon; T) \propto \varepsilon^2 / (\exp(\varepsilon/T) - 1)$.</p>
 *
 * <p><strong>Algorithm</strong> — exact mixture of Gamma distributions (Devroye 1986):
 * <pre>
 *   k ~ Zipf-like with P(k) ∝ 1/k³  (k = 1, …, 20)
 *   ε | k ~ Gamma(3, T/k)
 * </pre>
 * Mean $= 3\zeta(4)/\zeta(3) \cdot T \approx 2.701\,T$.</p>
 *
 * @param temperature Temperature $T$ in natural units ($kT$, $T > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Planck%27s_law">Planck's Law</a>
 */
class PlanckGenerator(
    private val temperature: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val cdf: DoubleArray = run {
        val w = DoubleArray(N_MAX) { 1.0 / (it + 1.0) / (it + 1.0) / (it + 1.0) }
        val sum = w.sum()
        var acc = 0.0
        DoubleArray(N_MAX) { i -> acc += w[i] / sum; acc }.also { it[N_MAX - 1] = 1.0 }
    }

    private val gammas: Array<GammaDistribution> =
        Array(N_MAX) { k -> GammaDistribution(randomGenerator, 3.0, temperature / (k + 1.0)) }

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        val k = cdf.indexOfFirst { it >= u }.let { if (it < 0) N_MAX - 1 else it }
        return gammas[k].sample()
    }

    companion object {
        private const val N_MAX = 20
        const val DISTRIBUTION_NAME = "planck"

        fun create(param1: Double?, generator: RandomGenerator): PlanckGenerator {
            val T = param1 ?: 1.0
            require(T > 0.0) { "Planck 'temperature' (param1) must be strictly positive." }
            return PlanckGenerator(T, generator)
        }
    }
}

@org.springframework.stereotype.Component
class PlanckFactory : DistributionFactory {
    override val name = PlanckGenerator.DISTRIBUTION_NAME
    override val description = "param1 = temperature T in kT units (def. 1.0) — blackbody photon energies"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        PlanckGenerator.create(p1, commonsRandom)
}
