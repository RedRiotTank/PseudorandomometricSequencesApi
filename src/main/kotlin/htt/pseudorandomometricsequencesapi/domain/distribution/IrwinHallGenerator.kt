package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following an
 * {@link <a href="https://en.wikipedia.org/wiki/Irwin%E2%80%93Hall_distribution">Irwin–Hall distribution</a>}.
 *
 * <p>The sum of $n$ independent $\mathcal{U}(0, 1)$ random variables: $X = \sum_{i=1}^{n} U_i$.
 * Support is $[0, n]$, mean $= n/2$, variance $= n/12$.</p>
 *
 * <p><strong>Algorithm</strong> — by definition (composition method):
 * <pre>
 *   return Σ Uᵢ   for i = 1 … n,  each Uᵢ ~ Uniform(0, 1)
 * </pre>
 * As $n$ grows, the Central Limit Theorem guarantees convergence to a Normal.
 * The classic trick $n = 12$ exploits this: $X_{12} - 6 \approx \mathcal{N}(0, 1)$
 * because $\text{Var}(X_{12}) = 12/12 = 1$.</p>
 *
 * @param n The number of uniform summands. Must be a positive integer.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Irwin%E2%80%93Hall_distribution">Irwin–Hall Distribution</a>
 */
class IrwinHallGenerator(
    private val n: Int,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Returns the sum of $n$ independent $\mathcal{U}(0,1)$ samples.
     */
    override fun sample(): Double {
        var sum = 0.0
        repeat(n) { sum += randomGenerator.nextDouble() }
        return sum
    }

    companion object {
        const val DISTRIBUTION_NAME = "irwin-hall"

        /**
         * Default: $n = 12$ — enables the $X_{12} - 6 \approx \mathcal{N}(0,1)$ approximation.
         */
        fun create(param1: Double?, generator: RandomGenerator): IrwinHallGenerator {
            val nDouble = param1 ?: 12.0
            require(nDouble > 0 && nDouble == nDouble.toInt().toDouble()) {
                "Irwin-Hall 'n' (param1) must be a positive integer."
            }
            return IrwinHallGenerator(nDouble.toInt(), generator)
        }
    }
}

@org.springframework.stereotype.Component
class IrwinHallFactory : DistributionFactory {
    override val name = IrwinHallGenerator.DISTRIBUTION_NAME
    override val description = "param1 = count n [integer] (def. 12) — sum of n uniforms; n=12 approximates N(6,1)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        IrwinHallGenerator.create(p1, commonsRandom)
}
