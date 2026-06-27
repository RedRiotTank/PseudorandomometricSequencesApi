package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Burr_distribution">Burr Type XII distribution</a>}.
 *
 * <p>A flexible two-parameter family on $[0, \infty)$ used in insurance, reliability and income
 * modelling. Many other distributions are special or limiting cases (Weibull, Logistic, Pareto,
 * Log-Logistic).</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   X = ((1−U)^(−1/k) − 1)^(1/c)
 * </pre></p>
 *
 * @param c First shape parameter ($c > 0$). Default 1.0.
 * @param k Second shape parameter ($k > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Burr_distribution">Burr Distribution</a>
 */
class BurrGenerator(
    private val c: Double,
    private val k: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = ((1-U)^{-1/k} - 1)^{1/c}$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return ((1.0 - u).pow(-1.0 / k) - 1.0).pow(1.0 / c)
    }

    companion object {
        const val DISTRIBUTION_NAME = "burr"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): BurrGenerator {
            val c = param1 ?: 1.0
            val k = param2 ?: 1.0
            require(c > 0.0) { "Burr 'c' (param1) must be strictly positive." }
            require(k > 0.0) { "Burr 'k' (param2) must be strictly positive." }
            return BurrGenerator(c, k, generator)
        }
    }
}

@org.springframework.stereotype.Component
class BurrFactory : DistributionFactory {
    override val name = BurrGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape c (def. 1.0), param2 = shape k (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BurrGenerator.create(p1, p2, commonsRandom)
}
