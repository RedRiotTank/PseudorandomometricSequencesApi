package htt.pseudorandomometricsequencesapi.domain.distribution

import java.util.Random

class UniformGenerator(
    private val min: Double,
    private val max: Double,
    private val javaRandom: Random
) : SequenceGenerator {

    init {
        require(min < max) { "Uniform 'min' (param1) must be less than 'max' (param2)." }
    }

    override fun sample(): Double {
        return min + (max - min) * javaRandom.nextDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "uniform"
        fun create(param1: Double?, param2: Double?, javaRandom: Random): UniformGenerator {
            val min = param1 ?: 0.0
            val max = param2 ?: 1.0

            return UniformGenerator(min, max, javaRandom)
        }
    }
}