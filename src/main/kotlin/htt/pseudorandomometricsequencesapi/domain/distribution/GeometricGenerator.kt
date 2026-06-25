package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GeometricDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Geometric_distribution">Geometric distribution</a>}.
 *
 * <p>This is a **discrete** distribution that models the number of failures before the first
 * success in a sequence of independent Bernoulli trials. The generated values are non-negative
 * integers $X \in \{0, 1, 2, \ldots\}$, where $X = 0$ means success on the first trial.</p>
 *
 * @param probabilityOfSuccess The probability of success in each trial ($p$). Must be $0 \lt p \le 1$.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Geometric_distribution">Geometric Distribution</a>
 */
class GeometricGenerator(
    probabilityOfSuccess: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: GeometricDistribution = GeometricDistribution(randomGenerator, probabilityOfSuccess)

    override fun sample(): Double = distribution.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "geometric"

        fun create(param1: Double?, generator: RandomGenerator): GeometricGenerator {
            val probability = param1 ?: 0.5
            require(probability > 0.0 && probability <= 1.0) {
                "Geometric 'probability' (param1) must be in the range (0.0, 1.0]."
            }
            return GeometricGenerator(probability, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GeometricFactory : DistributionFactory {
    override val name = GeometricGenerator.DISTRIBUTION_NAME
    override val description = "param1 = probability p (def. 0.5) — models number of failures before first success"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GeometricGenerator.create(p1, commonsRandom)
}
