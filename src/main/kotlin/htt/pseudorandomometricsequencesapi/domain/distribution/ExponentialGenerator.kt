package htt.pseudorandomometricsequencesapi.domain.distribution

import java.util.Random
import kotlin.math.ln

class ExponentialGenerator(
    private val lambda: Double,
    private val javaRandom: Random
) : SequenceGenerator {

    init {
        require(lambda > 0.0) { "Exponential 'lambda' (param1) must be positive." }
    }

    override fun sample(): Double {
        return -1.0 / lambda * ln(1.0 - javaRandom.nextDouble())
    }

    companion object {
        const val DISTRIBUTION_NAME = "exponential"
        fun create(param1: Double?, javaRandom: Random): ExponentialGenerator {
            val lambda = param1 ?: 1.0

            return ExponentialGenerator(lambda, javaRandom)
        }
    }
}