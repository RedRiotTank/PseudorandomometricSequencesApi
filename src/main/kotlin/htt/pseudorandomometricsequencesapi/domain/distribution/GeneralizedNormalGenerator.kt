package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Generalized_normal_distribution">Generalized Normal distribution</a>}
 * (Subbotin / Exponential Power distribution).
 *
 * <p>Generalises the Normal distribution via a shape parameter $\beta$ that controls tail weight:</p>
 * <ul>
 *   <li>$\beta = 2$: Gaussian (Normal)</li>
 *   <li>$\beta = 1$: Laplace (Double Exponential)</li>
 *   <li>$\beta \to \infty$: Uniform on $[\mu-\sigma, \mu+\sigma]$</li>
 * </ul>
 * <p>Used in signal processing (sparse coding, ICA) and robust statistics.</p>
 *
 * <p><strong>Algorithm</strong>:
 * <pre>
 *   W ~ Gamma(1/β, 1)
 *   sign = +1 if U ≥ 0.5 else −1
 *   X = μ + σ · sign · W^(1/β)
 * </pre></p>
 *
 * @param location Location parameter ($\mu$). Default 0.0.
 * @param scale Scale parameter ($\sigma > 0$). Default 1.0.
 * @param shape Shape parameter ($\beta > 0$). Default 2.0 (Gaussian).
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Generalized_normal_distribution">Generalized Normal Distribution</a>
 */
class GeneralizedNormalGenerator(
    private val location: Double,
    private val scale: Double,
    private val shape: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val gamma = GammaDistribution(randomGenerator, 1.0 / shape, 1.0)

    override fun sample(): Double {
        val w = gamma.sample()
        val sign = if (randomGenerator.nextDouble() >= 0.5) 1.0 else -1.0
        return location + scale * sign * w.pow(1.0 / shape)
    }

    companion object {
        const val DISTRIBUTION_NAME = "generalized-normal"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): GeneralizedNormalGenerator {
            val mu   = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            val beta  = param3 ?: 2.0
            require(sigma > 0.0) { "Generalized Normal 'scale' (param2) must be strictly positive." }
            require(beta  > 0.0) { "Generalized Normal 'shape' (param3) must be strictly positive." }
            return GeneralizedNormalGenerator(mu, sigma, beta, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GeneralizedNormalFactory : DistributionFactory {
    override val name = GeneralizedNormalGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = scale σ (def. 1.0), param3 = shape β (def. 2.0; 2=Gaussian, 1=Laplace)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GeneralizedNormalGenerator.create(p1, p2, p3, commonsRandom)
}
