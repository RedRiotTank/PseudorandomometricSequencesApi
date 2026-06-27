package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Arcsine_distribution">Arcsine distribution</a>}.
 *
 * <p>Defined on $[a, b]$ with PDF $f(x) = \frac{1}{\pi\sqrt{(x-a)(b-x)}}$, which is
 * U-shaped — probability mass concentrates near the endpoints. Arises naturally in the
 * arc-sine law of random walks: the fraction of time a symmetric random walk spends above
 * zero follows this distribution.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   U ~ Uniform(0, 1)
 *   return a + (b − a) · sin²(π·U/2)
 * </pre></p>
 *
 * @param a Lower bound. Default 0.0.
 * @param b Upper bound. Must satisfy $b > a$. Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Arcsine_distribution">Arcsine Distribution</a>
 */
class ArcSineGenerator(
    private val a: Double,
    private val b: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        val s = sin(PI * u / 2.0)
        return a + (b - a) * s * s
    }

    companion object {
        const val DISTRIBUTION_NAME = "arcsine"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ArcSineGenerator {
            val a = param1 ?: 0.0
            val b = param2 ?: 1.0
            require(a < b) { "ArcSine lower bound (param1=$a) must be strictly less than upper bound (param2=$b)." }
            return ArcSineGenerator(a, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ArcSineFactory : DistributionFactory {
    override val name = ArcSineGenerator.DISTRIBUTION_NAME
    override val description = "param1 = lower bound a (def. 0.0), param2 = upper bound b (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ArcSineGenerator.create(p1, p2, commonsRandom)
}
