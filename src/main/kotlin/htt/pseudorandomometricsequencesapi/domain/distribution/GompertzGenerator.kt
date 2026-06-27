package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Gompertz_distribution">Gompertz distribution</a>}.
 *
 * <p>Models mortality and failure rates that increase exponentially with time. Widely used in
 * survival analysis, actuarial science, customer churn modelling, and biological ageing. When
 * $b \to 0$ it converges to an Exponential distribution.</p>
 *
 * <p><strong>Algorithm</strong> — exact inverse-CDF:
 * <pre>
 *   X = (1/b) · ln(1 − (b/η) · ln(U))
 * </pre></p>
 *
 * @param eta Shape parameter ($\eta > 0$). Default 1.0.
 * @param b Rate parameter ($b > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Gompertz_distribution">Gompertz Distribution</a>
 */
class GompertzGenerator(
    private val eta: Double,
    private val b: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return (1.0 / b) * ln(1.0 - (b / eta) * ln(u))
    }

    companion object {
        const val DISTRIBUTION_NAME = "gompertz"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): GompertzGenerator {
            val eta = param1 ?: 1.0
            val b   = param2 ?: 1.0
            require(eta > 0.0) { "Gompertz 'eta' (param1) must be strictly positive." }
            require(b   > 0.0) { "Gompertz 'b' (param2) must be strictly positive." }
            return GompertzGenerator(eta, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class GompertzFactory : DistributionFactory {
    override val name = GompertzGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape η (def. 1.0), param2 = rate b (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        GompertzGenerator.create(p1, p2, commonsRandom)
}
