package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Yule%E2%80%93Simon_distribution">Yule–Simon distribution</a>}.
 *
 * <p>A discrete power-law distribution on $\{1, 2, 3, \ldots\}$ with PMF
 * $P(X = k) = \rho \cdot B(k, \rho + 1)$ and recurrence
 * $P(X = k+1) = P(X = k) \cdot k / (k + \rho + 1)$.</p>
 *
 * <p>Models preferential-attachment phenomena: city sizes, species abundance, citation counts,
 * and word frequency (the "rich get richer" effect). Mean $= \rho/(\rho-1)$ for $\rho > 1$.</p>
 *
 * <p><strong>Algorithm</strong> — sequential inverse-CDF using the recurrence.</p>
 *
 * @param rho Shape parameter ($\rho > 0$). Default 1.5.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Yule%E2%80%93Simon_distribution">Yule–Simon Distribution</a>
 */
class YuleSimonGenerator(
    private val rho: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        var k = 1
        var prob = rho / (rho + 1.0)          // P(X=1)
        var cumulative = prob
        while (cumulative < u) {
            k++
            prob *= (k - 1).toDouble() / (k + rho)
            cumulative += prob
        }
        return k.toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "yule-simon"

        fun create(param1: Double?, generator: RandomGenerator): YuleSimonGenerator {
            val rho = param1 ?: 1.5
            require(rho > 0.0) { "Yule-Simon 'rho' (param1) must be strictly positive." }
            return YuleSimonGenerator(rho, generator)
        }
    }
}

@org.springframework.stereotype.Component
class YuleSimonFactory : DistributionFactory {
    override val name = YuleSimonGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape ρ (def. 1.5) — power-law; mean = ρ/(ρ-1) for ρ > 1"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        YuleSimonGenerator.create(p1, commonsRandom)
}
