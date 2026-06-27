package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.tan

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Cauchy_distribution#Half-Cauchy_distribution">Half-Cauchy distribution</a>}.
 *
 * <p>The distribution of $|X|$ where $X \sim \text{Cauchy}(0, \gamma)$. Defined on $[0, \infty)$
 * with very heavy tails (no finite moments). Used extensively as a weakly informative prior for
 * scale parameters in Bayesian hierarchical models, particularly in Stan and PyMC.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   X = γ · tan(π·U/2)
 * </pre>
 * Median $= \gamma$.</p>
 *
 * @param scale Scale parameter ($\gamma > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cauchy_distribution">Cauchy Distribution</a>
 */
class HalfCauchyGenerator(
    private val scale: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return scale * tan(PI * u / 2.0)
    }

    companion object {
        const val DISTRIBUTION_NAME = "half-cauchy"

        fun create(param1: Double?, generator: RandomGenerator): HalfCauchyGenerator {
            val gamma = param1 ?: 1.0
            require(gamma > 0.0) { "Half-Cauchy 'scale' (param1) must be strictly positive." }
            return HalfCauchyGenerator(gamma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class HalfCauchyFactory : DistributionFactory {
    override val name = HalfCauchyGenerator.DISTRIBUTION_NAME
    override val description = "param1 = scale γ (def. 1.0) — positive half of Cauchy; common Bayesian scale prior"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        HalfCauchyGenerator.create(p1, commonsRandom)
}
