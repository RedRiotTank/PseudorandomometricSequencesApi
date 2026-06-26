package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Logarithmic_distribution">Logarithmic distribution</a>}
 * (also known as the Log-Series distribution).
 *
 * <p>A discrete distribution with PMF $P(X = k) = -\frac{1}{\ln(1-p)} \cdot \frac{p^k}{k}$
 * for $k = 1, 2, 3, \ldots$ Arises as the distribution of species abundance in ecology
 * (Fisher's log-series) and in the compound Poisson-Logarithmic model for count data.</p>
 *
 * <p>Sampling uses the sequential CDF inversion method exploiting the recurrence
 * $P(X = k+1) = P(X = k) \cdot p \cdot k / (k+1)$.</p>
 *
 * @param p The distribution parameter ($0 \lt p \lt 1$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Logarithmic_distribution">Logarithmic Distribution</a>
 */
class LogarithmicGenerator(
    private val p: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val logQ: Double = ln(1.0 - p)

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        var k = 1
        var prob = -p / logQ
        var cumulative = prob
        while (cumulative < u) {
            k++
            prob *= p * (k - 1).toDouble() / k
            cumulative += prob
        }
        return k.toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "logarithmic"

        /**
         * Default: $p = 0.5$.
         */
        fun create(param1: Double?, generator: RandomGenerator): LogarithmicGenerator {
            val p = param1 ?: 0.5
            require(p > 0.0 && p < 1.0) { "Logarithmic 'p' (param1) must be in the open interval (0.0, 1.0)." }
            return LogarithmicGenerator(p, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LogarithmicFactory : DistributionFactory {
    override val name = LogarithmicGenerator.DISTRIBUTION_NAME
    override val description = "param1 = probability p in (0,1) (def. 0.5)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LogarithmicGenerator.create(p1, commonsRandom)
}
