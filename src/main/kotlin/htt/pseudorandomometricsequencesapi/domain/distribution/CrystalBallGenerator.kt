package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Crystal_Ball_function">Crystal Ball distribution</a>}.
 *
 * <p>The most widely used empirical distribution in High Energy Physics (HEP). Combines a
 * Gaussian core on the right with a power-law tail on the left, modelling detector resolution
 * effects such as radiative energy loss via bremsstrahlung in calorimeters. Ubiquitous in
 * invariant-mass peak fits at the LHC, B-factories, and LEP experiments.</p>
 *
 * <p>Standard form (μ=0, σ=1): the transition occurs at z = −α.</p>
 *
 * <p><strong>Algorithm</strong> — exact composition sampler:
 * <pre>
 *   p_gauss = ∫_{−α}^∞ exp(−z²/2) dz / total_area
 *   if U &lt; p_gauss: z ~ TruncatedNormal(0,1) above −α
 *   else:           z = (n/α)(1 − V^{−1/(n−1)}) − α   [power-law tail]
 *   return μ + σ·z
 * </pre></p>
 *
 * @param alpha Transition point ($\alpha > 0$). Default 1.5.
 * @param n Power-law index ($n > 1$). Default 2.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Crystal_Ball_function">Crystal Ball function</a>
 */
class CrystalBallGenerator(
    private val alpha: Double,
    private val n: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val normal = NormalDistribution(randomGenerator, 0.0, 1.0)

    // Area of the Gaussian piece (∫_{-α}^∞ exp(-z²/2) dz)
    private val gaussArea = sqrt(2.0 * PI) * normal.cumulativeProbability(alpha)
    // Area of the power-law tail
    private val powerArea = n * exp(-alpha * alpha / 2.0) / (alpha * (n - 1.0))
    private val pGauss   = gaussArea / (gaussArea + powerArea)
    private val uLow     = normal.cumulativeProbability(-alpha)

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        val z = if (u < pGauss) {
            normal.inverseCumulativeProbability(uLow + randomGenerator.nextDouble() * (1.0 - uLow))
        } else {
            val v = randomGenerator.nextDouble().coerceAtLeast(1e-15)
            (n / alpha) * (1.0 - v.pow(-1.0 / (n - 1.0))) - alpha
        }
        return z
    }

    companion object {
        const val DISTRIBUTION_NAME = "crystal-ball"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): CrystalBallGenerator {
            val alpha = param1 ?: 1.5
            val n     = param2 ?: 2.0
            require(alpha > 0.0) { "Crystal Ball 'alpha' (param1) must be strictly positive." }
            require(n     > 1.0) { "Crystal Ball 'n' (param2) must be greater than 1." }
            return CrystalBallGenerator(alpha, n, generator)
        }
    }
}

@org.springframework.stereotype.Component
class CrystalBallFactory : DistributionFactory {
    override val name = CrystalBallGenerator.DISTRIBUTION_NAME
    override val description = "param1 = transition α > 0 (def. 1.5), param2 = power-law index n > 1 (def. 2.0) — standard form μ=0 σ=1"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        CrystalBallGenerator.create(p1, p2, commonsRandom)
}
