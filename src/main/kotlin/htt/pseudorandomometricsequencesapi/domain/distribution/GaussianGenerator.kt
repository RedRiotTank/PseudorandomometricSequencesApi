package htt.pseudorandomometricsequencesapi.domain.distribution

import java.util.Random

class GaussianGenerator(
    private val mean: Double,
    private val stddev: Double,
    private val javaRandom: Random
) : SequenceGenerator {

    init {
        require(stddev > 0.0) { "Gaussian 'stddev' (param2) must be positive." }
    }

     override fun sample(): Double {
        return mean + stddev * javaRandom.nextGaussian()
    }

    companion object {
        const val DISTRIBUTION_NAME = "gaussian"
        fun create(param1: Double?, param2: Double?, javaRandom: Random): GaussianGenerator {
            val mean = param1 ?: 0.0
            val stddev = param2 ?: 1.0

            return GaussianGenerator(mean, stddev, javaRandom)
        }
    }
}