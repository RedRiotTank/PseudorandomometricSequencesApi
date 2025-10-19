package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.random.RandomGenerator

class GammaGenerator(
    shape: Double,
    scale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {
    val distribution: GammaDistribution

    init {
        require(shape > 0.0) { "Gamma 'shape' (param1) must be positive." }
        require(scale > 0.0) { "Gamma 'scale' (param2) must be positive." }

        this.distribution = GammaDistribution(randomGenerator, shape, scale)
    }

    override fun sample(): Double {
        return distribution.sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "gamma"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): GammaGenerator {
            val shape = param1 ?: 1.0
            val scale = param2 ?: 1.0


            return GammaGenerator(shape, scale, generator)
        }
    }
}