package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.distribution.PoissonDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Noncentral_chi-squared_distribution">Noncentral Chi-Squared distribution</a>}
 * $\chi^2(k, \lambda)$.
 *
 * <p>Generalises the Chi-Squared distribution by adding a noncentrality parameter $\lambda \ge 0$.
 * Used in statistical power analysis and in modelling signal power in communications.
 * When $\lambda = 0$ it reduces to $\chi^2(k)$.</p>
 *
 * <p><strong>Algorithm</strong> — exact Poisson mixing:
 * <pre>
 *   M ~ Poisson(λ/2)
 *   X ~ Gamma((k + 2M)/2, 2)   ≡  χ²(k + 2M)
 * </pre>
 * Mean $= k + \lambda$, Variance $= 2(k + 2\lambda)$.</p>
 *
 * @param df Degrees of freedom ($k > 0$). Default 3.0.
 * @param noncentrality Noncentrality parameter ($\lambda \ge 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Noncentral_chi-squared_distribution">Noncentral Chi-Squared</a>
 */
class NoncentralChiSquaredGenerator(
    private val df: Double,
    private val noncentrality: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    // PoissonDistribution requires mean > 0, so only instantiate when λ > 0
    private val poissonDist: PoissonDistribution? = if (noncentrality > 0.0) PoissonDistribution(
        randomGenerator, noncentrality / 2.0,
        PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS
    ) else null

    override fun sample(): Double {
        val m = poissonDist?.sample() ?: 0
        return GammaDistribution(randomGenerator, (df + 2.0 * m) / 2.0, 2.0).sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "noncentral-chi-squared"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): NoncentralChiSquaredGenerator {
            val df = param1 ?: 3.0
            val lambda = param2 ?: 1.0
            require(df > 0.0) { "Noncentral Chi-Squared 'df' (param1) must be strictly positive." }
            require(lambda >= 0.0) { "Noncentral Chi-Squared 'noncentrality' (param2) must be non-negative." }
            return NoncentralChiSquaredGenerator(df, lambda, generator)
        }
    }
}

@org.springframework.stereotype.Component
class NoncentralChiSquaredFactory : DistributionFactory {
    override val name = NoncentralChiSquaredGenerator.DISTRIBUTION_NAME
    override val description = "param1 = degrees of freedom k (def. 3.0), param2 = non-centrality λ ≥ 0 (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        NoncentralChiSquaredGenerator.create(p1, p2, commonsRandom)
}
