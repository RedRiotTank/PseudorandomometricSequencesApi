package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.random.RandomGenerator

class LogNormalGenerator(
    mu: Double,
    sigma: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {
    val distribution: LogNormalDistribution

    init {
        require(sigma > 0.0) { "LogNormal 'sigma' (param2) must be positive." }

        this.distribution = LogNormalDistribution(randomGenerator, mu, sigma)
    }

    override fun sample(): Double {
        return distribution.sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "lognormal"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LogNormalGenerator {
            val mu = param1 ?: 0.0
            val sigma = param2 ?: 1.0

            return LogNormalGenerator(mu, sigma, generator)
        }
    }
}