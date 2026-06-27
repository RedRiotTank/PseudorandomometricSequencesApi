package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following an
 * {@link <a href="https://en.wikipedia.org/wiki/Erlang_distribution">Erlang distribution</a>}.
 *
 * <p>A special case of the Gamma distribution restricted to positive-integer shape values.
 * Models the waiting time until the $k$-th arrival in a Poisson process with rate $\lambda$.
 * Fundamental in queuing theory (Erlang B and C models) and telecommunications.</p>
 *
 * <p>Backed by Apache Commons Math's {@code GammaDistribution} with shape $k$ (integer) and
 * scale $1/\lambda$.</p>
 *
 * @param shape The shape parameter ($k$). Must be a positive integer.
 * @param rate The rate parameter ($\lambda$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Erlang_distribution">Erlang Distribution</a>
 */
class ErlangGenerator(
    shape: Int,
    rate: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    val distribution: GammaDistribution = GammaDistribution(randomGenerator, shape.toDouble(), 1.0 / rate)

    override fun sample(): Double = distribution.sample()

    companion object {
        const val DISTRIBUTION_NAME = "erlang"

        /**
         * Default: $k = 1$, $\lambda = 1.0$ (equivalent to Exponential(1)).
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ErlangGenerator {
            val kDouble = param1 ?: 1.0
            val lambda = param2 ?: 1.0
            require(kDouble > 0 && kDouble == kDouble.toInt().toDouble()) {
                "Erlang 'shape' (param1) must be a positive integer."
            }
            require(lambda > 0.0) { "Erlang 'rate' (param2) must be strictly positive." }
            return ErlangGenerator(kDouble.toInt(), lambda, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ErlangFactory : DistributionFactory {
    override val name = ErlangGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape k [integer] (def. 1), param2 = rate λ (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ErlangGenerator.create(p1, p2, commonsRandom)
}
