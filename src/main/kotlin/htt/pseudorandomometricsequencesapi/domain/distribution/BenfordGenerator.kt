package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.floor
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following
 * {@link <a href="https://en.wikipedia.org/wiki/Benford%27s_law">Benford's Law</a>}
 * (also known as the First-Digit Law).
 *
 * <p>In many naturally occurring datasets (populations, financial figures, physical constants),
 * the leading significant digit $d$ appears with probability
 * $P(D = d) = \log_{10}(1 + 1/d)$ for $d \in \{1, 2, \ldots, 9\}$.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF via the identity
 * $F(d) = \log_{10}(d + 1)$, so $d = \lfloor 10^U \rfloor$ for $U \sim \mathcal{U}(0, 1)$:
 * <pre>
 *   U ~ Uniform(0, 1)
 *   return floor(10^U)        // ∈ {1, …, 9}
 * </pre>
 * Derivation: $P(d = k) = P(k \le 10^U < k+1) = P(\log_{10} k \le U < \log_{10}(k+1))
 * = \log_{10}(1 + 1/k)$ ✓</p>
 *
 * <p>Applications: forensic accounting (fraud detection), data quality checks, and
 * validation of scientific measurements.</p>
 *
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Benford%27s_law">Benford's Law</a>
 */
class BenfordGenerator(
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Returns a leading digit in $\{1, \ldots, 9\}$ distributed according to Benford's Law.
     *
     * <p>Exact inverse-CDF: $d = \lfloor 10^U \rfloor$, clamped to $[1, 9]$ to handle
     * the degenerate case $U = 1$.</p>
     */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return floor(10.0.pow(u)).coerceIn(1.0, 9.0)
    }

    companion object {
        const val DISTRIBUTION_NAME = "benford"

        /** No parameters. */
        fun create(generator: RandomGenerator): BenfordGenerator = BenfordGenerator(generator)
    }
}

@org.springframework.stereotype.Component
class BenfordFactory : DistributionFactory {
    override val name = BenfordGenerator.DISTRIBUTION_NAME
    override val description = "no parameters — returns leading digit in {1,...,9} following Benford's law"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BenfordGenerator.create(commonsRandom)
}
