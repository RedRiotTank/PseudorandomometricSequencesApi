package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Log-logistic_distribution">Log-Logistic distribution</a>}
 * (also known as the Fisk distribution).
 *
 * <p>The distribution of a random variable whose logarithm follows a Logistic distribution.
 * Widely used in survival analysis (event-time modelling), economics (income distribution),
 * and hydrology.</p>
 *
 * <p>Sampling uses the exact inverse-CDF: $X = \alpha \left(\frac{U}{1-U}\right)^{1/\beta}$.</p>
 *
 * @param scale The scale parameter ($\alpha$). Must be strictly positive.
 * @param shape The shape parameter ($\beta$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Log-logistic_distribution">Log-Logistic Distribution</a>
 */
class LogLogisticGenerator(
    private val scale: Double,
    private val shape: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = \alpha (U / (1-U))^{1/\beta}$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return scale * (u / (1.0 - u)).pow(1.0 / shape)
    }

    companion object {
        const val DISTRIBUTION_NAME = "log-logistic"

        /**
         * Default: $\alpha = 1.0$ (scale), $\beta = 1.0$ (shape).
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LogLogisticGenerator {
            val alpha = param1 ?: 1.0
            val beta = param2 ?: 1.0
            require(alpha > 0.0) { "Log-Logistic 'scale' (param1) must be strictly positive." }
            require(beta > 0.0) { "Log-Logistic 'shape' (param2) must be strictly positive." }
            return LogLogisticGenerator(alpha, beta, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LogLogisticFactory : DistributionFactory {
    override val name = LogLogisticGenerator.DISTRIBUTION_NAME
    override val description = "param1 = scale α (def. 1.0), param2 = shape β (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LogLogisticGenerator.create(p1, p2, commonsRandom)
}
