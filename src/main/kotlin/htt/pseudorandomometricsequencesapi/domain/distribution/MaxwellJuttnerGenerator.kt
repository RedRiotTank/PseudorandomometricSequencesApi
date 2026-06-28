package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Maxwell%E2%80%93J%C3%BCttner_distribution">Maxwell–Jüttner distribution</a>}.
 *
 * <p>The relativistic generalisation of the Maxwell–Boltzmann speed distribution for an
 * ideal gas at temperature $\theta = kT/(mc^2)$. Describes momentum magnitudes in
 * relativistic plasmas, quark-gluon plasma simulations, and astrophysical jets.
 * Reduces to Maxwell–Boltzmann for $\theta \ll 1$.</p>
 *
 * <p>PDF (natural units $m=c=1$): $f(p; \theta) \propto p^2 \exp(-\sqrt{1+p^2}/\theta)$.</p>
 *
 * <p><strong>Algorithm</strong> — acceptance-rejection with Gamma(3, θ) proposal:
 * <pre>
 *   p ~ Gamma(3, θ)   [non-relativistic Maxwell-Boltzmann shape]
 *   accept with w(p) = exp(−(√(1+p²) − p)/θ) ∈ (0, 1]
 * </pre></p>
 *
 * @param theta Dimensionless temperature ($\theta = kT/mc^2 > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Maxwell%E2%80%93J%C3%BCttner_distribution">Maxwell–Jüttner Distribution</a>
 */
class MaxwellJuttnerGenerator(
    private val theta: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val proposal = GammaDistribution(randomGenerator, 3.0, theta)

    override fun sample(): Double {
        while (true) {
            val p      = proposal.sample()
            val weight = exp(-(sqrt(1.0 + p * p) - p) / theta)
            if (randomGenerator.nextDouble() < weight) return p
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "maxwell-juttner"

        fun create(param1: Double?, generator: RandomGenerator): MaxwellJuttnerGenerator {
            val theta = param1 ?: 1.0
            require(theta > 0.0) { "Maxwell-Jüttner 'theta' (param1) must be strictly positive." }
            return MaxwellJuttnerGenerator(theta, generator)
        }
    }
}

@org.springframework.stereotype.Component
class MaxwellJuttnerFactory : DistributionFactory {
    override val name = MaxwellJuttnerGenerator.DISTRIBUTION_NAME
    override val description = "param1 = dimensionless temperature θ = kT/mc² (def. 1.0) — relativistic Maxwell-Boltzmann"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        MaxwellJuttnerGenerator.create(p1, commonsRandom)
}
