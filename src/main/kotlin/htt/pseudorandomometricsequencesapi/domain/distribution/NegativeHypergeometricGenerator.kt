package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Negative_hypergeometric_distribution">Negative Hypergeometric distribution</a>}.
 *
 * <p>Models the number of failures drawn <em>without replacement</em> from a finite population
 * of size $N$ (containing $K$ successes and $N - K$ failures) until exactly $r$ successes are
 * observed. The discrete-without-replacement counterpart of the Negative Binomial distribution.</p>
 *
 * <p>Mean $= r(N - K) / (K + 1)$.</p>
 *
 * <p><strong>Algorithm</strong> — direct simulation of draws without replacement.</p>
 *
 * @param n Population size ($N$, positive integer). Default 50.
 * @param k Successes in population ($K$, $0 \le K \le N$, integer). Default 25.
 * @param r Target successes ($r$, positive integer, $r \le K$). Default 5.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Negative_hypergeometric_distribution">Negative Hypergeometric Distribution</a>
 */
class NegativeHypergeometricGenerator(
    private val n: Int,
    private val k: Int,
    private val r: Int,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        var successes = 0
        var failures = 0
        var remaining = n
        var remainingSuccesses = k
        while (successes < r) {
            if (randomGenerator.nextDouble() * remaining < remainingSuccesses) {
                successes++
                remainingSuccesses--
            } else {
                failures++
            }
            remaining--
        }
        return failures.toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "negative-hypergeometric"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): NegativeHypergeometricGenerator {
            val nDouble = param1 ?: 50.0
            val kDouble = param2 ?: 25.0
            val rDouble = param3 ?: 5.0
            require(nDouble > 0 && nDouble == nDouble.toInt().toDouble()) {
                "Negative Hypergeometric 'N' (param1) must be a positive integer."
            }
            require(kDouble >= 0 && kDouble == kDouble.toInt().toDouble()) {
                "Negative Hypergeometric 'K' (param2) must be a non-negative integer."
            }
            require(rDouble > 0 && rDouble == rDouble.toInt().toDouble()) {
                "Negative Hypergeometric 'r' (param3) must be a positive integer."
            }
            val nInt = nDouble.toInt(); val kInt = kDouble.toInt(); val rInt = rDouble.toInt()
            require(kInt <= nInt) { "Negative Hypergeometric K (param2=$kInt) cannot exceed N (param1=$nInt)." }
            require(rInt <= kInt) { "Negative Hypergeometric r (param3=$rInt) cannot exceed K (param2=$kInt)." }
            return NegativeHypergeometricGenerator(nInt, kInt, rInt, generator)
        }
    }
}

@org.springframework.stereotype.Component
class NegativeHypergeometricFactory : DistributionFactory {
    override val name = NegativeHypergeometricGenerator.DISTRIBUTION_NAME
    override val description = "param1 = population N [integer] (def. 50), param2 = successes K [integer] (def. 25), param3 = target successes r [integer] (def. 5)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        NegativeHypergeometricGenerator.create(p1, p2, p3, commonsRandom)
}
