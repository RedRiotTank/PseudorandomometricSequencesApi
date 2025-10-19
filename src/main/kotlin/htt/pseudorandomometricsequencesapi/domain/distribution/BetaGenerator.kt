package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.random.RandomGenerator

class BetaGenerator(
    alpha: Double,
    beta: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {
    val distribution: BetaDistribution

    init {
        require(alpha > 0.0) { "Beta 'alpha' (param1) must be positive." }
        require(beta > 0.0) { "Beta 'beta' (param2) must be positive." }

        this.distribution = BetaDistribution(randomGenerator, alpha, beta)
    }

    override fun sample(): Double {
        return distribution.sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "beta"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): BetaGenerator {
            val alpha = param1 ?: 1.0
            val beta = param2 ?: 1.0

            return BetaGenerator(alpha, beta, generator)
        }
    }
}