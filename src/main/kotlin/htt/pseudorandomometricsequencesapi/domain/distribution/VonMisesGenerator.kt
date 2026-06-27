package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.*

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Von_Mises_distribution">Von Mises distribution</a>}.
 *
 * <p>The circular analogue of the Normal distribution, defined on $(-\pi, \pi]$. The concentration
 * parameter $\kappa$ controls how tightly the distribution clusters around the mean direction $\mu$:
 * $\kappa = 0$ gives a uniform circular distribution, while large $\kappa$ approximates a Gaussian.</p>
 *
 * <p>Sampling uses the Best–Fisher (1979) accept-reject algorithm.</p>
 *
 * @param mu The mean direction ($\mu$), in radians.
 * @param kappa The concentration parameter ($\kappa \ge 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Von_Mises_distribution">Von Mises Distribution</a>
 */
class VonMisesGenerator(
    private val mu: Double,
    private val kappa: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Best–Fisher (1979) accept-reject sampler.
     * Returns a value in $(-\pi, \pi]$.
     */
    override fun sample(): Double {
        if (kappa == 0.0) {
            return randomGenerator.nextDouble() * 2.0 * PI - PI
        }
        val a = 1.0 + sqrt(1.0 + 4.0 * kappa * kappa)
        val b = (a - sqrt(2.0 * a)) / (2.0 * kappa)
        val r = (1.0 + b * b) / (2.0 * b)

        while (true) {
            val u1 = randomGenerator.nextDouble()
            val z = cos(PI * u1)
            val f = (1.0 + r * z) / (r + z)
            val c = kappa * (r - f)
            val u2 = randomGenerator.nextDouble()
            if (c * (2.0 - c) - u2 > 0.0 || ln(c / u2) + 1.0 - c >= 0.0) {
                val u3 = randomGenerator.nextDouble()
                val theta = sign(u3 - 0.5) * acos(f) + mu
                // wrap to (-π, π]
                var result = theta % (2.0 * PI)
                if (result > PI) result -= 2.0 * PI
                if (result <= -PI) result += 2.0 * PI
                return result
            }
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "von-mises"

        /**
         * Default: $\mu = 0.0$ rad, $\kappa = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): VonMisesGenerator {
            val mu = param1 ?: 0.0
            val kappa = param2 ?: 1.0
            require(kappa >= 0.0) { "Von Mises 'concentration' (param2) must be non-negative." }
            return VonMisesGenerator(mu, kappa, generator)
        }
    }
}

@org.springframework.stereotype.Component
class VonMisesFactory : DistributionFactory {
    override val name = VonMisesGenerator.DISTRIBUTION_NAME
    override val description = "param1 = mean direction μ [radians] (def. 0.0), param2 = concentration κ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        VonMisesGenerator.create(p1, p2, commonsRandom)
}
