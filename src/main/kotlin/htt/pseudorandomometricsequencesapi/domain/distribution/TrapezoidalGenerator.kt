package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Trapezoidal_distribution">Trapezoidal distribution</a>}.
 *
 * <p>Generalises the Triangular and Uniform distributions. The PDF has a rising slope from
 * $a$ to $b$, a flat plateau from $b$ to $c$, and a falling slope from $c$ to $d$. Useful in
 * project-risk modelling (PERT variants) and expert-knowledge elicitation when a range is
 * "most likely" rather than a single peak.</p>
 *
 * <p>The plateau fraction $f \in (0, 1]$ controls the width of the flat top as a fraction of
 * the total range: $b = a + \frac{1-f}{2}(d-a)$, $c = d - \frac{1-f}{2}(d-a)$.</p>
 * <ul>
 *   <li>$f = 1$: Uniform on $[a, d]$</li>
 *   <li>$f \to 0$: Triangular with peak at $(a+d)/2$</li>
 * </ul>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF using the three regions.</p>
 *
 * @param a Lower bound. Default 0.0.
 * @param d Upper bound ($d > a$). Default 1.0.
 * @param plateauFrac Flat-top fraction in $(0, 1]$. Default 0.5.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Trapezoidal_distribution">Trapezoidal Distribution</a>
 */
class TrapezoidalGenerator(
    private val a: Double,
    private val d: Double,
    private val plateauFrac: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val b = a + (1.0 - plateauFrac) / 2.0 * (d - a)
    private val c = d - (1.0 - plateauFrac) / 2.0 * (d - a)
    private val h = 2.0 / (d - a + c - b)
    private val u1 = h * (b - a) / 2.0
    private val u2 = u1 + h * (c - b)

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return when {
            u < u1 -> a + sqrt(2.0 * (b - a) * u / h)
            u < u2 -> b + (u - u1) / h
            else   -> d - sqrt(2.0 * (d - c) * (1.0 - u) / h)
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "trapezoidal"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): TrapezoidalGenerator {
            val a    = param1 ?: 0.0
            val d    = param2 ?: 1.0
            val frac = param3 ?: 0.5
            require(a < d)              { "Trapezoidal lower bound (param1=$a) must be less than upper bound (param2=$d)." }
            require(frac > 0.0 && frac <= 1.0) { "Trapezoidal plateau fraction (param3) must be in (0, 1]." }
            return TrapezoidalGenerator(a, d, frac, generator)
        }
    }
}

@org.springframework.stereotype.Component
class TrapezoidalFactory : DistributionFactory {
    override val name = TrapezoidalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = min a (def. 0.0), param2 = max d (def. 1.0), param3 = plateau fraction in (0,1] (def. 0.5; 1=Uniform, →0=Triangular)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        TrapezoidalGenerator.create(p1, p2, p3, commonsRandom)
}
