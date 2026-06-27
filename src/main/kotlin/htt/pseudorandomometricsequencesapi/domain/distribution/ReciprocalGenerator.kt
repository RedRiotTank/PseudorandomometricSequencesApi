package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.exp
import kotlin.math.ln

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Reciprocal_distribution">Reciprocal (Log-Uniform) distribution</a>}.
 *
 * <p>Density is proportional to $1/x$ on $[a, b]$, making it uniform on a logarithmic scale.
 * Used as an uninformative prior for scale parameters in Bayesian analysis (Jeffreys prior),
 * and to model quantities spanning several orders of magnitude.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   X = a · (b/a)^U = a · exp(U · ln(b/a))
 * </pre></p>
 *
 * @param a Lower bound ($a > 0$). Default 0.1.
 * @param b Upper bound ($b > a$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Reciprocal_distribution">Reciprocal Distribution</a>
 */
class ReciprocalGenerator(
    private val a: Double,
    private val b: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val logRatio: Double = ln(b / a)

    /** Inverse-CDF: $X = a \cdot \exp(U \cdot \ln(b/a))$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return a * exp(u * logRatio)
    }

    companion object {
        const val DISTRIBUTION_NAME = "reciprocal"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ReciprocalGenerator {
            val a = param1 ?: 0.1
            val b = param2 ?: 1.0
            require(a > 0.0) { "Reciprocal lower bound (param1) must be strictly positive." }
            require(b > a) { "Reciprocal upper bound (param2=$b) must be greater than lower bound (param1=$a)." }
            return ReciprocalGenerator(a, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ReciprocalFactory : DistributionFactory {
    override val name = ReciprocalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = lower bound a > 0 (def. 0.1), param2 = upper bound b (def. 1.0) — uniform on log scale"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ReciprocalGenerator.create(p1, p2, commonsRandom)
}
