package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Maxwell%E2%80%93Boltzmann_distribution">Maxwell–Boltzmann distribution</a>}.
 *
 * <p>Describes the speed distribution of particles in an ideal gas at thermal equilibrium
 * (Maxwell's kinetic theory of gases, 1860). A particle's speed is the Euclidean norm of
 * its 3D velocity vector, whose components are independent zero-mean Gaussians.</p>
 *
 * <p><strong>Algorithm</strong> — by definition as a 3D Gaussian magnitude:
 * <pre>
 *   X₁, X₂, X₃  ~  N(0, 1)  independently
 *   return σ · √(X₁² + X₂² + X₃²)
 * </pre>
 * This is equivalent to $\sigma \cdot \chi(3)$ (Chi distribution with $k = 3$ d.f.).</p>
 *
 * @param scale The scale parameter ($\sigma = \sqrt{k_B T / m}$ in physics). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Maxwell%E2%80%93Boltzmann_distribution">Maxwell–Boltzmann Distribution</a>
 */
class MaxwellBoltzmannGenerator(
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Samples the speed as the norm of a 3D Gaussian velocity vector.
     */
    override fun sample(): Double {
        val x1 = randomGenerator.nextGaussian()
        val x2 = randomGenerator.nextGaussian()
        val x3 = randomGenerator.nextGaussian()
        return scale * sqrt(x1 * x1 + x2 * x2 + x3 * x3)
    }

    companion object {
        const val DISTRIBUTION_NAME = "maxwell-boltzmann"

        /**
         * Default: $\sigma = 1.0$.
         */
        fun create(param1: Double?, generator: RandomGenerator): MaxwellBoltzmannGenerator {
            val sigma = param1 ?: 1.0
            require(sigma > 0.0) { "Maxwell-Boltzmann 'scale' (param1) must be strictly positive." }
            return MaxwellBoltzmannGenerator(sigma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class MaxwellBoltzmannFactory : DistributionFactory {
    override val name = MaxwellBoltzmannGenerator.DISTRIBUTION_NAME
    override val description = "param1 = scale σ (def. 1.0) — models speed of gas molecules"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        MaxwellBoltzmannGenerator.create(p1, commonsRandom)
}
