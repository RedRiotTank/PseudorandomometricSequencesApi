package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Generalized_gamma_distribution">Generalized Gamma distribution</a>}
 * (Stacy distribution).
 *
 * <p>A three-parameter family that unifies many common distributions:</p>
 * <ul>
 *   <li>$p = 1$: Gamma$(d, a)$</li>
 *   <li>$d = p$: Weibull</li>
 *   <li>$d = 1, p = 2$: Rayleigh (with $a = \sigma\sqrt{2}$)</li>
 *   <li>$d = 1, p = 2, a = 1$: Maxwell-Boltzmann-like</li>
 *   <li>$d = p \to \infty$: Log-Normal (limiting case)</li>
 * </ul>
 *
 * <p><strong>Algorithm</strong>:
 * <pre>
 *   G ~ Gamma(d/p, 1)
 *   X = a · G^(1/p)
 * </pre></p>
 *
 * @param a Scale parameter ($a > 0$). Default 1.0.
 * @param d Shape parameter ($d > 0$). Default 1.0.
 * @param p Power parameter ($p > 0$). Default 1.0 (reduces to Gamma).
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Generalized_gamma_distribution">Generalized Gamma Distribution</a>
 */
class GeneralizedGammaGenerator(
    private val a: Double,
    private val d: Double,
    private val p: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val gamma = GammaDistribution(randomGenerator, d / p, 1.0)

    override fun sample(): Double = a * gamma.sample().pow(1.0 / p)

    companion object {
        const val DISTRIBUTION_NAME = "generalized-gamma"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): GeneralizedGammaGenerator {
            val a = param1 ?: 1.0
            val d = param2 ?: 1.0
            val p = param3 ?: 1.0
            require(a > 0.0) { "Generalized Gamma 'scale a' (param1) must be strictly positive." }
            require(d > 0.0) { "Generalized Gamma 'shape d' (param2) must be strictly positive." }
            require(p > 0.0) { "Generalized Gamma 'power p' (param3) must be strictly positive." }
            return GeneralizedGammaGenerator(a, d, p, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GeneralizedGammaFactory : DistributionFactory {
    override val name = GeneralizedGammaGenerator.DISTRIBUTION_NAME
    override val description = "param1 = scale a (def. 1.0), param2 = shape d (def. 1.0), param3 = power p (def. 1.0; p=1=Gamma, d=p=Weibull)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GeneralizedGammaGenerator.create(p1, p2, p3, commonsRandom)
}
