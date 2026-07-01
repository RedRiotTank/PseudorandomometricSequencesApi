package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Relativistic_Breit%E2%80%93Wigner_distribution">Relativistic Breit–Wigner distribution</a>}.
 *
 * <p>Describes the invariant-mass lineshape of unstable particles (resonances) produced in
 * particle collisions. The relativistic version (in terms of $m$, not $E$) is used for Z boson,
 * Higgs, $\rho$ meson, $J/\psi$, and virtually all particle resonance fits at the LHC and LEP.
 * The non-relativistic version reduces to Cauchy (Lorentzian).</p>
 *
 * <p>PDF: $f(m; m_0, \Gamma) \propto \frac{m_0\,\Gamma}{(m^2-m_0^2)^2 + m_0^2\Gamma^2}$
 * for $m \ge 0$.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF on $[0, \infty)$:
 * <pre>
 *   u_low  = (arctan(−m₀/Γ) + π/2) / π
 *   u_scaled = u_low + U·(1 − u_low)
 *   m² = m₀² + m₀Γ·tan(π(u_scaled − 1/2))
 *   m  = √(max(0, m²))
 * </pre></p>
 *
 * @param mass Pole mass ($m_0 > 0$). Default 1.0.
 * @param width Decay width ($\Gamma > 0$). Default 0.1.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Relativistic_Breit%E2%80%93Wigner_distribution">Relativistic Breit–Wigner</a>
 */
class RelativisticBreitWignerGenerator(
    private val mass: Double,
    private val width: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val uLow = (PI / 2.0 - atan(mass / width)) / PI

    override fun sample(): Double {
        val u      = uLow + randomGenerator.nextDouble() * (1.0 - uLow)
        val m2     = mass * mass + mass * width * tan(PI * (u - 0.5))
        return sqrt(max(0.0, m2))
    }

    companion object {
        const val DISTRIBUTION_NAME = "relativistic-breit-wigner"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): RelativisticBreitWignerGenerator {
            val m0    = param1 ?: 1.0
            val gamma = param2 ?: 0.1
            require(m0    > 0.0) { "Relativistic Breit-Wigner 'mass' (param1) must be strictly positive." }
            require(gamma > 0.0) { "Relativistic Breit-Wigner 'width' (param2) must be strictly positive." }
            return RelativisticBreitWignerGenerator(m0, gamma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class RelativisticBreitWignerFactory : DistributionFactory {
    override val name = RelativisticBreitWignerGenerator.DISTRIBUTION_NAME
    override val description = "param1 = pole mass m₀ (def. 1.0), param2 = decay width Γ (def. 0.1) — particle resonances"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        RelativisticBreitWignerGenerator.create(p1, p2, commonsRandom)
}
