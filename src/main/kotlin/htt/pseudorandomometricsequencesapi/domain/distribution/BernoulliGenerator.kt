package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.BinomialDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Bernoulli_distribution">Bernoulli distribution</a>}.
 *
 * <p>This is a **discrete** distribution that models a single trial with two possible outcomes:
 * success (1) with probability $p$ and failure (0) with probability $1 - p$. It is a special
 * case of the Binomial distribution with $n = 1$.</p>
 *
 * @param probabilityOfSuccess The probability of success ($p$). Must be $0 \le p \le 1$.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bernoulli_distribution">Bernoulli Distribution</a>
 */
class BernoulliGenerator(
    probabilityOfSuccess: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: BinomialDistribution = BinomialDistribution(randomGenerator, 1, probabilityOfSuccess)

    /**
     * @return {@code 1.0} with probability $p$ or {@code 0.0} with probability $1 - p$.
     */
    override fun sample(): Double = distribution.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "bernoulli"

        fun create(param1: Double?, generator: RandomGenerator): BernoulliGenerator {
            val probability = param1 ?: 0.5
            require(probability in 0.0..1.0) {
                "Bernoulli 'probability' (param1) must be between 0.0 and 1.0."
            }
            return BernoulliGenerator(probability, generator)
        }
    }
}

@org.springframework.stereotype.Component
class BernoulliFactory : DistributionFactory {
    override val name = BernoulliGenerator.DISTRIBUTION_NAME
    override val description = "param1 = probability p (def. 0.5)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BernoulliGenerator.create(p1, commonsRandom)
}
