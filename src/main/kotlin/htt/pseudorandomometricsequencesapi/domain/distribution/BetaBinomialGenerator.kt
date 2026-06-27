package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Beta-binomial_distribution">Beta-Binomial distribution</a>}.
 *
 * <p>A compound distribution where the success probability of a Binomial is itself random,
 * drawn from a Beta distribution. Handles over-dispersed count data when the Binomial assumption
 * of a fixed probability is too restrictive. Applied in A/B testing, epidemiology, and genetics.</p>
 *
 * <p><strong>Algorithm</strong> — by definition (two-step):
 * <pre>
 *   p ~ Beta(α, β)
 *   X ~ Binomial(n, p)   [via n Bernoulli trials]
 * </pre>
 * Mean $= n\alpha/(\alpha+\beta)$.</p>
 *
 * @param n Number of trials (positive integer). Default 10.
 * @param alpha First Beta shape parameter ($\alpha > 0$). Default 1.0.
 * @param beta Second Beta shape parameter ($\beta > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Beta-binomial_distribution">Beta-Binomial Distribution</a>
 */
class BetaBinomialGenerator(
    private val n: Int,
    alpha: Double,
    beta: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val betaDist: BetaDistribution = BetaDistribution(randomGenerator, alpha, beta)

    override fun sample(): Double {
        val p = betaDist.sample()
        var count = 0
        repeat(n) { if (randomGenerator.nextDouble() < p) count++ }
        return count.toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "beta-binomial"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): BetaBinomialGenerator {
            val nDouble = param1 ?: 10.0
            val alpha = param2 ?: 1.0
            val beta = param3 ?: 1.0
            require(nDouble > 0 && nDouble == nDouble.toInt().toDouble()) {
                "Beta-Binomial 'n' (param1) must be a positive integer."
            }
            require(alpha > 0.0) { "Beta-Binomial 'alpha' (param2) must be strictly positive." }
            require(beta > 0.0) { "Beta-Binomial 'beta' (param3) must be strictly positive." }
            return BetaBinomialGenerator(nDouble.toInt(), alpha, beta, generator)
        }
    }
}

@org.springframework.stereotype.Component
class BetaBinomialFactory : DistributionFactory {
    override val name = BetaBinomialGenerator.DISTRIBUTION_NAME
    override val description = "param1 = trials n [integer] (def. 10), param2 = Beta shape α (def. 1.0), param3 = Beta shape β (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BetaBinomialGenerator.create(p1, p2, p3, commonsRandom)
}
