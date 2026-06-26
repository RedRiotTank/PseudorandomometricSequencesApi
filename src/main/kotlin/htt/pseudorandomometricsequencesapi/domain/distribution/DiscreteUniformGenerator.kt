package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Discrete_uniform_distribution">Discrete Uniform distribution</a>}.
 *
 * <p>Each integer in the inclusive range $[a, b]$ is equally likely. The continuous
 * {@code UniformGenerator} covers real-valued uniform sampling; this generator covers the
 * integer counterpart used in dice rolls, random indexing, and combinatorial sampling.</p>
 *
 * @param lower The inclusive lower bound ($a$).
 * @param upper The inclusive upper bound ($b$). Must satisfy $b \ge a$.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Discrete_uniform_distribution">Discrete Uniform Distribution</a>
 */
class DiscreteUniformGenerator(
    lower: Int,
    upper: Int,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: UniformIntegerDistribution = UniformIntegerDistribution(randomGenerator, lower, upper)

    override fun sample(): Double = distribution.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "discrete-uniform"

        /**
         * Default: $a = 0$, $b = 9$ (simulates a single decimal digit).
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): DiscreteUniformGenerator {
            val aDouble = param1 ?: 0.0
            val bDouble = param2 ?: 9.0
            require(aDouble == aDouble.toInt().toDouble()) {
                "Discrete Uniform 'lower' (param1) must be an integer."
            }
            require(bDouble == bDouble.toInt().toDouble()) {
                "Discrete Uniform 'upper' (param2) must be an integer."
            }
            val a = aDouble.toInt(); val b = bDouble.toInt()
            require(b >= a) { "Discrete Uniform 'upper' (param2=$b) must be >= 'lower' (param1=$a)." }
            return DiscreteUniformGenerator(a, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class DiscreteUniformFactory : DistributionFactory {
    override val name = DiscreteUniformGenerator.DISTRIBUTION_NAME
    override val description = "param1 = lower bound a [integer] (def. 0), param2 = upper bound b [integer] (def. 9)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        DiscreteUniformGenerator.create(p1, p2, commonsRandom)
}
