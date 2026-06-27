package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Scaled_inverse_chi-squared_distribution">Scaled Inverse Chi-Squared distribution</a>}
 * $\text{Scale-Inv-}\chi^2(\nu, \tau^2)$.
 *
 * <p>The standard conjugate prior for the variance parameter of a Normal distribution in
 * Bayesian statistics. When $\tau^2 = 1/\nu$ it reduces to the Inverse Chi-Squared distribution.</p>
 *
 * <p><strong>Algorithm</strong> — via the Chi-Squared relationship:
 * <pre>
 *   if X ~ χ²(ν),  then  Y = ν·τ² / X  ~  Scale-Inv-χ²(ν, τ²)
 * </pre>
 * Mean $= \tau^2\nu/(\nu-2)$ for $\nu > 2$.</p>
 *
 * @param nu Degrees of freedom ($\nu > 0$). Default 3.0.
 * @param scaleSq Scale parameter ($\tau^2 > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Scaled_inverse_chi-squared_distribution">Scaled Inverse Chi-Squared</a>
 */
class ScaledInverseChiSquaredGenerator(
    private val nu: Double,
    private val scaleSq: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val chiSquared: ChiSquaredDistribution = ChiSquaredDistribution(randomGenerator, nu)

    override fun sample(): Double = nu * scaleSq / chiSquared.sample()

    companion object {
        const val DISTRIBUTION_NAME = "scaled-inverse-chi-squared"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ScaledInverseChiSquaredGenerator {
            val nu = param1 ?: 3.0
            val tau2 = param2 ?: 1.0
            require(nu > 0.0) { "Scaled Inverse Chi-Squared 'nu' (param1) must be strictly positive." }
            require(tau2 > 0.0) { "Scaled Inverse Chi-Squared 'scaleSq' (param2) must be strictly positive." }
            return ScaledInverseChiSquaredGenerator(nu, tau2, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ScaledInverseChiSquaredFactory : DistributionFactory {
    override val name = ScaledInverseChiSquaredGenerator.DISTRIBUTION_NAME
    override val description = "param1 = degrees of freedom ν (def. 3.0), param2 = scale τ² (def. 1.0) — conjugate prior for Normal variance"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ScaledInverseChiSquaredGenerator.create(p1, p2, commonsRandom)
}
