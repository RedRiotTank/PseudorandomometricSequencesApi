package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.PascalDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Negative_binomial_distribution">Negative Binomial distribution</a>}.
 *
 * <p>This is a **discrete** distribution that models the number of failures before the $r$-th
 * success in a sequence of independent Bernoulli trials. It generalises the Geometric distribution
 * (special case $r = 1$). Backed by Apache Commons Math's {@code PascalDistribution}.</p>
 *
 * @param numberOfSuccesses The target number of successes ($r$). Must be a positive integer.
 * @param probabilityOfSuccess The probability of success in each trial ($p$). Must be $0 \lt p \le 1$.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Negative_binomial_distribution">Negative Binomial Distribution</a>
 */
class NegativeBinomialGenerator(
    numberOfSuccesses: Int,
    probabilityOfSuccess: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: PascalDistribution = PascalDistribution(randomGenerator, numberOfSuccesses, probabilityOfSuccess)

    override fun sample(): Double = distribution.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "negative-binomial"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): NegativeBinomialGenerator {
            val rDouble = param1 ?: 1.0
            val probability = param2 ?: 0.5
            require(rDouble > 0 && rDouble == rDouble.toInt().toDouble()) {
                "Negative Binomial 'numberOfSuccesses' (param1) must be a positive integer."
            }
            require(probability > 0.0 && probability <= 1.0) {
                "Negative Binomial 'probability' (param2) must be in the range (0.0, 1.0]."
            }
            return NegativeBinomialGenerator(rDouble.toInt(), probability, generator)
        }
    }
}

@org.springframework.stereotype.Component
class NegativeBinomialFactory : DistributionFactory {
    override val name = NegativeBinomialGenerator.DISTRIBUTION_NAME
    override val description = "param1 = successes r [integer] (def. 1), param2 = probability p (def. 0.5)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        NegativeBinomialGenerator.create(p1, p2, commonsRandom)
}
