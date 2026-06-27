package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Generalized_Pareto_distribution">Generalized Pareto distribution</a>}
 * (GPD).
 *
 * <p>Unifies the families of distributions used for modelling exceedances over a threshold.
 * The shape parameter $\xi$ controls tail behaviour:</p>
 * <ul>
 *   <li>$\xi = 0$: Exponential (light tail)</li>
 *   <li>$\xi > 0$: Pareto-type (heavy tail)</li>
 *   <li>$\xi < 0$: Beta-type (bounded upper tail)</li>
 * </ul>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   if ξ ≠ 0:  X = μ + σ/ξ · ((1−U)^(−ξ) − 1)
 *   if ξ = 0:  X = μ − σ · ln(1−U)
 * </pre></p>
 *
 * @param location Location parameter ($\mu$). Default 0.0.
 * @param scale Scale parameter ($\sigma > 0$). Default 1.0.
 * @param shape Shape parameter ($\xi$). Default 0.0 (Exponential).
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Generalized_Pareto_distribution">GPD</a>
 */
class GeneralizedParetoGenerator(
    private val location: Double,
    private val scale: Double,
    private val shape: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return if (Math.abs(shape) < 1e-10) {
            location - scale * ln(1.0 - u)
        } else {
            location + scale / shape * ((1.0 - u).pow(-shape) - 1.0)
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "generalized-pareto"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): GeneralizedParetoGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            val xi = param3 ?: 0.0
            require(sigma > 0.0) { "Generalized Pareto 'scale' (param2) must be strictly positive." }
            return GeneralizedParetoGenerator(mu, sigma, xi, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GeneralizedParetoFactory : DistributionFactory {
    override val name = GeneralizedParetoGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0), param3 = shape ξ (def. 0.0; 0=Exponential, >0=heavy tail, <0=bounded)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GeneralizedParetoGenerator.create(p1, p2, p3, commonsRandom)
}
