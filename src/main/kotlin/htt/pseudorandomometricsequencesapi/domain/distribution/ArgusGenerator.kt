package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/ARGUS_distribution">ARGUS distribution</a>}.
 *
 * <p>Named after the ARGUS experiment at DESY (Hamburg). Describes the continuum background
 * in the invariant-mass distribution of B-meson decay products near the kinematic endpoint
 * $c$ (typically $c \approx m_{B^*}$ or $c \approx E_{\text{beam}}$). Also used in BaBar,
 * Belle, and LHCb analyses.</p>
 *
 * <p>PDF: $f(x; c, \chi) \propto x\,\sqrt{1-(x/c)^2}\,\exp\!\left(-\tfrac{\chi^2}{2}(1-(x/c)^2)\right)$
 * for $0 \le x \le c$.</p>
 *
 * <p><strong>Algorithm</strong> — acceptance-rejection with uniform envelope on $[0, c]$:
 * <pre>
 *   propose x ~ Uniform(0, c)
 *   accept if U &lt; (x/c) · √(1−(x/c)²) · exp(−χ²(1−(x/c)²)/2) / 0.5
 * </pre></p>
 *
 * @param c Kinematic endpoint ($c > 0$). Default 1.0.
 * @param chi Shape parameter ($\chi \ge 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/ARGUS_distribution">ARGUS Distribution</a>
 */
class ArgusGenerator(
    private val c: Double,
    private val chi: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val chi2 = chi * chi

    override fun sample(): Double {
        while (true) {
            val x  = randomGenerator.nextDouble() * c
            val t  = x / c
            val t2 = t * t
            val accept = t * sqrt(1.0 - t2) * exp(-chi2 * (1.0 - t2) / 2.0) / 0.5
            if (randomGenerator.nextDouble() < accept) return x
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "argus"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ArgusGenerator {
            val c   = param1 ?: 1.0
            val chi = param2 ?: 1.0
            require(c   > 0.0) { "ARGUS 'endpoint c' (param1) must be strictly positive." }
            require(chi >= 0.0) { "ARGUS 'chi' (param2) must be non-negative." }
            return ArgusGenerator(c, chi, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ArgusFactory : DistributionFactory {
    override val name = ArgusGenerator.DISTRIBUTION_NAME
    override val description = "param1 = endpoint c (def. 1.0), param2 = shape χ ≥ 0 (def. 1.0) — B-meson continuum background"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ArgusGenerator.create(p1, p2, commonsRandom)
}
