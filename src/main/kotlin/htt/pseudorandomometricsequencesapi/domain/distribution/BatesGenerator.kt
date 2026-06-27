package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Bates_distribution">Bates distribution</a>}.
 *
 * <p>The mean of $n$ independent $\mathcal{U}(0,1)$ random variables: $X = \frac{1}{n}\sum_{i=1}^n U_i$.
 * Support is $[0, 1]$, mean $= 0.5$, variance $= \frac{1}{12n}$. Related to the Irwin-Hall
 * distribution by $\text{Bates}(n) = \text{IrwinHall}(n)/n$.</p>
 *
 * <p>The default $n = 12$ is historically significant: $\sqrt{12}\,(X_{12} - 0.5) \approx \mathcal{N}(0,1)$
 * was once used as a fast Normal approximation before Box-Muller transforms were widespread.</p>
 *
 * <p><strong>Algorithm</strong>:
 * <pre>
 *   return (Σ Uᵢ for i=1..n) / n
 * </pre></p>
 *
 * @param n Number of uniform summands (positive integer). Default 12.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bates_distribution">Bates Distribution</a>
 */
class BatesGenerator(
    private val n: Int,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        var sum = 0.0
        repeat(n) { sum += randomGenerator.nextDouble() }
        return sum / n
    }

    companion object {
        const val DISTRIBUTION_NAME = "bates"

        fun create(param1: Double?, generator: RandomGenerator): BatesGenerator {
            val nDouble = param1 ?: 12.0
            require(nDouble > 0 && nDouble == nDouble.toInt().toDouble()) {
                "Bates 'n' (param1) must be a positive integer."
            }
            return BatesGenerator(nDouble.toInt(), generator)
        }
    }
}

@org.springframework.stereotype.Component
class BatesFactory : DistributionFactory {
    override val name = BatesGenerator.DISTRIBUTION_NAME
    override val description = "param1 = count n [integer] (def. 12) — mean of n uniforms; output in [0,1]"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BatesGenerator.create(p1, commonsRandom)
}
