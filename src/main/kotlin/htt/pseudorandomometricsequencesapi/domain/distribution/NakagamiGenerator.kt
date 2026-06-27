package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.NakagamiDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Nakagami_distribution">Nakagami distribution</a>}.
 *
 * <p>Generalises the Rayleigh distribution and is widely used to model signal amplitude in
 * wireless fading channels. Special cases: $m = 0.5$ gives a Half-Normal, $m = 1$ gives Rayleigh,
 * and large $m$ approximates a Gaussian.</p>
 *
 * @param mu The shape parameter ($m \ge 0.5$).
 * @param omega The spread parameter ($\Omega \gt 0$), equal to the mean squared value.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Nakagami_distribution">Nakagami Distribution</a>
 */
class NakagamiGenerator(
    mu: Double,
    omega: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: NakagamiDistribution =
        NakagamiDistribution(randomGenerator, mu, omega, NakagamiDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "nakagami"

        /**
         * Default: $m = 1.0$ (Rayleigh), $\Omega = 1.0$.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): NakagamiGenerator {
            val m = param1 ?: 1.0
            val omega = param2 ?: 1.0
            require(m >= 0.5) { "Nakagami 'mu' (param1) must be >= 0.5." }
            require(omega > 0.0) { "Nakagami 'omega' (param2) must be strictly positive." }
            return NakagamiGenerator(m, omega, generator)
        }
    }
}

@org.springframework.stereotype.Component
class NakagamiFactory : DistributionFactory {
    override val name = NakagamiGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape m ≥ 0.5 (def. 1.0), param2 = spread Ω (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        NakagamiGenerator.create(p1, p2, commonsRandom)
}
