package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Tsallis_statistics">Tsallis distribution</a>}
 * (q-exponential / Tsallis–Pareto).
 *
 * <p>Describes the transverse-momentum ($p_T$) spectra of hadrons produced in high-energy
 * proton–proton and heavy-ion collisions at the LHC and RHIC. The parameter $n$ controls
 * the power-law tail: larger $n$ gives softer tails.</p>
 *
 * <p>PDF: $f(x; T, n) \propto \left(1 + x / [(n-1)T]\right)^{-n}$ for $x \ge 0$.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF (identical to Lomax with
 * $\alpha = n-1$, $\lambda = (n-1)T$):
 * <pre>
 *   X = (n−1)·T · ((1−U)^{−1/(n−1)} − 1)
 * </pre>
 * Mean $= (n-1)T / (n-2)$ for $n > 2$.</p>
 *
 * @param T Temperature parameter ($T > 0$). Default 0.1 (GeV, typical for light hadrons).
 * @param n Exponent ($n > 1$). Default 7.0 (typical pp value at LHC).
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Tsallis_statistics">Tsallis Statistics</a>
 */
class TsallisGenerator(
    private val T: Double,
    private val n: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return (n - 1.0) * T * ((1.0 - u).pow(-1.0 / (n - 1.0)) - 1.0)
    }

    companion object {
        const val DISTRIBUTION_NAME = "tsallis"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): TsallisGenerator {
            val T = param1 ?: 0.1
            val n = param2 ?: 7.0
            require(T > 0.0) { "Tsallis 'T' (param1) must be strictly positive." }
            require(n > 1.0) { "Tsallis 'n' (param2) must be greater than 1." }
            return TsallisGenerator(T, n, generator)
        }
    }
}

@org.springframework.stereotype.Component
class TsallisFactory : DistributionFactory {
    override val name = TsallisGenerator.DISTRIBUTION_NAME
    override val description = "param1 = temperature T (def. 0.1 GeV), param2 = exponent n > 1 (def. 7.0) — hadron pT spectra"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        TsallisGenerator.create(p1, p2, commonsRandom)
}
