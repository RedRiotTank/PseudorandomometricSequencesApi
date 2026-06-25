package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.distribution.PascalDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Beta_negative_binomial_distribution">Beta-Negative Binomial distribution</a>}.
 *
 * <p>A compound distribution where the success probability of a Negative Binomial is drawn
 * from a Beta distribution: $p \sim \text{Beta}(\alpha, \beta)$, then $X \sim \text{NegBin}(r, p)$.
 * Models over-dispersed count data when both the number of failures and the success probability
 * are uncertain. Generalises the Beta-Binomial, Negative Binomial, and Geometric distributions.</p>
 *
 * <p>Mean $= r\beta / (\alpha - 1)$ for $\alpha > 1$.</p>
 *
 * @param r Target successes ($r$, positive integer). Default 1.
 * @param alpha First Beta shape ($\alpha > 0$). Default 2.0.
 * @param betaParam Second Beta shape ($\beta > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Beta_negative_binomial_distribution">Beta-Negative Binomial Distribution</a>
 */
class BetaNegativeBinomialGenerator(
    private val r: Int,
    alpha: Double,
    betaParam: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val betaDist = BetaDistribution(randomGenerator, alpha, betaParam)

    override fun sample(): Double {
        val p = betaDist.sample().coerceIn(1e-10, 1.0 - 1e-10)
        return PascalDistribution(randomGenerator, r, p).sample().toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "beta-negative-binomial"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): BetaNegativeBinomialGenerator {
            val rDouble = param1 ?: 1.0
            val alpha   = param2 ?: 2.0
            val beta    = param3 ?: 1.0
            require(rDouble > 0 && rDouble == rDouble.toInt().toDouble()) {
                "Beta-Negative Binomial 'r' (param1) must be a positive integer."
            }
            require(alpha > 0.0) { "Beta-Negative Binomial 'alpha' (param2) must be strictly positive." }
            require(beta  > 0.0) { "Beta-Negative Binomial 'beta' (param3) must be strictly positive." }
            return BetaNegativeBinomialGenerator(rDouble.toInt(), alpha, beta, generator)
        }
    }
}

@org.springframework.stereotype.Component
class BetaNegativeBinomialFactory : DistributionFactory {
    override val name = BetaNegativeBinomialGenerator.DISTRIBUTION_NAME
    override val description = "param1 = successes r [integer] (def. 1), param2 = Beta shape α (def. 2.0), param3 = Beta shape β (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BetaNegativeBinomialGenerator.create(p1, p2, p3, commonsRandom)
}
