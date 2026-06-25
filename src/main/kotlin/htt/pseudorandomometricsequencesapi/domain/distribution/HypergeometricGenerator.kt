package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.HypergeometricDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Hypergeometric_distribution">Hypergeometric distribution</a>}.
 *
 * <p>This is a **discrete** distribution that models the number of successes in $n$ draws
 * **without replacement** from a finite population of size $N$ containing exactly $K$ successes.</p>
 *
 * @param populationSize The total population size ($N$). Must be a positive integer.
 * @param numberOfSuccesses The number of success states in the population ($K$). Must be $0 \le K \le N$.
 * @param sampleSize The number of draws ($n$). Must be $0 \le n \le N$.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Hypergeometric_distribution">Hypergeometric Distribution</a>
 */
class HypergeometricGenerator(
    populationSize: Int,
    numberOfSuccesses: Int,
    sampleSize: Int,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: HypergeometricDistribution =
        HypergeometricDistribution(randomGenerator, populationSize, numberOfSuccesses, sampleSize)

    override fun sample(): Double = distribution.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "hypergeometric"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): HypergeometricGenerator {
            val nDouble = param1 ?: 100.0
            val kDouble = param2 ?: 50.0
            val sDouble = param3 ?: 10.0
            require(nDouble > 0 && nDouble == nDouble.toInt().toDouble()) {
                "Hypergeometric 'populationSize' (param1) must be a positive integer."
            }
            require(kDouble >= 0 && kDouble == kDouble.toInt().toDouble()) {
                "Hypergeometric 'numberOfSuccesses' (param2) must be a non-negative integer."
            }
            require(sDouble >= 0 && sDouble == sDouble.toInt().toDouble()) {
                "Hypergeometric 'sampleSize' (param3) must be a non-negative integer."
            }
            val n = nDouble.toInt(); val k = kDouble.toInt(); val s = sDouble.toInt()
            require(k <= n) { "Hypergeometric 'numberOfSuccesses' (param2=$k) cannot exceed 'populationSize' (param1=$n)." }
            require(s <= n) { "Hypergeometric 'sampleSize' (param3=$s) cannot exceed 'populationSize' (param1=$n)." }
            return HypergeometricGenerator(n, k, s, generator)
        }
    }
}

@org.springframework.stereotype.Component
class HypergeometricFactory : DistributionFactory {
    override val name = HypergeometricGenerator.DISTRIBUTION_NAME
    override val description = "param1 = population N [integer] (def. 100), param2 = successes K [integer] (def. 50), param3 = draws n [integer] (def. 10)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        HypergeometricGenerator.create(p1, p2, p3, commonsRandom)
}
