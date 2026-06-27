package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Skew_normal_distribution">Skew-Normal distribution</a>}
 * $\text{SN}(\xi, \omega, \alpha)$.
 *
 * <p>Generalises the Normal distribution by introducing a shape parameter $\alpha$ that controls
 * skewness: $\alpha = 0$ recovers $\mathcal{N}(\xi, \omega^2)$; $\alpha \to +\infty$ gives a
 * Half-Normal; $\alpha < 0$ produces a left-skewed distribution.</p>
 *
 * <p><strong>Algorithm</strong> — Azzalini & Capitanio stochastic representation:
 * <pre>
 *   δ  = α / √(1 + α²)
 *   Z₁ ~ N(0, 1),  Z₂ ~ N(0, 1)  independently
 *   W  = δ·|Z₁| + √(1 − δ²)·Z₂       // W ~ SN(0, 1, α)
 *   return ξ + ω·W
 * </pre>
 * Derivation: $W$ is a linear combination of a folded Normal ($|Z_1|$) and an independent
 * Normal ($Z_2$). The mixing weight $\delta$ controls how much asymmetry is injected.</p>
 *
 * @param location The location parameter ($\xi$). Default 0.0.
 * @param scale The scale parameter ($\omega$). Must be strictly positive.
 * @param shape The shape (skewness) parameter ($\alpha$). Default 0.0 (symmetric Normal).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Skew_normal_distribution">Skew-Normal Distribution</a>
 */
class SkewNormalGenerator(
    private val location: Double,
    private val scale: Double,
    private val shape: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    // δ = α / √(1 + α²)  — precomputed for efficiency
    private val delta: Double = shape / sqrt(1.0 + shape * shape)
    private val deltaComplement: Double = sqrt(1.0 - delta * delta)

    /**
     * Samples via the stochastic representation $W = \delta |Z_1| + \sqrt{1-\delta^2} Z_2$.
     */
    override fun sample(): Double {
        val z1 = randomGenerator.nextGaussian()
        val z2 = randomGenerator.nextGaussian()
        val w = delta * abs(z1) + deltaComplement * z2
        return location + scale * w
    }

    companion object {
        const val DISTRIBUTION_NAME = "skew-normal"

        /**
         * Default: $\xi = 0.0$, $\omega = 1.0$, $\alpha = 0.0$ (standard Normal).
         */
        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): SkewNormalGenerator {
            val xi = param1 ?: 0.0
            val omega = param2 ?: 1.0
            val alpha = param3 ?: 0.0
            require(omega > 0.0) { "Skew-Normal 'scale' (param2) must be strictly positive." }
            return SkewNormalGenerator(xi, omega, alpha, generator)
        }
    }
}

@org.springframework.stereotype.Component
class SkewNormalFactory : DistributionFactory {
    override val name = SkewNormalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location ξ (def. 0.0), param2 = scale ω (def. 1.0), param3 = shape α (def. 0.0; 0=symmetric Normal)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        SkewNormalGenerator.create(p1, p2, p3, commonsRandom)
}
