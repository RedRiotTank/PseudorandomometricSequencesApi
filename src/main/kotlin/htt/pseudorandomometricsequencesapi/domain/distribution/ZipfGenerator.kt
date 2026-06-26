package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.ZipfDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Zipf%27s_law">Zipf distribution</a>}.
 *
 * <p>A discrete power-law distribution where $P(X = k) \propto k^{-s}$. Describes many
 * real-world phenomena: word frequency in natural language (Zipf's law), city populations,
 * website traffic, and income distribution.</p>
 *
 * @param numberOfElements The number of elements ($n$). Must be a positive integer.
 * @param exponent The exponent of the distribution ($s \gt 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Zipf%27s_law">Zipf Distribution</a>
 */
class ZipfGenerator(
    numberOfElements: Int,
    exponent: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: ZipfDistribution = ZipfDistribution(randomGenerator, numberOfElements, exponent)

    override fun sample(): Double = distribution.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "zipf"

        /**
         * Default: $n = 10$ (elements), $s = 1.0$ (exponent).
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ZipfGenerator {
            val nDouble = param1 ?: 10.0
            val s = param2 ?: 1.0
            require(nDouble > 0 && nDouble == nDouble.toInt().toDouble()) {
                "Zipf 'numberOfElements' (param1) must be a positive integer."
            }
            require(s > 0.0) { "Zipf 'exponent' (param2) must be strictly positive." }
            return ZipfGenerator(nDouble.toInt(), s, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ZipfFactory : DistributionFactory {
    override val name = ZipfGenerator.DISTRIBUTION_NAME
    override val description = "param1 = number of elements n [integer] (def. 10), param2 = exponent s (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ZipfGenerator.create(p1, p2, commonsRandom)
}
