package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Kumaraswamy_distribution">Kumaraswamy distribution</a>}.
 *
 * <p>A two-parameter family on $(0, 1)$ similar to the Beta distribution but with a closed-form
 * CDF and PDF, making it more tractable analytically. Used in hydrology, simulation, and
 * Bayesian modelling as an alternative prior for probabilities.</p>
 *
 * <p>Sampling uses the exact inverse-CDF: $X = \left(1 - (1-U)^{1/b}\right)^{1/a}$.</p>
 *
 * @param a The first shape parameter ($a \gt 0$).
 * @param b The second shape parameter ($b \gt 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Kumaraswamy_distribution">Kumaraswamy Distribution</a>
 */
class KumaraswamyGenerator(
    private val a: Double,
    private val b: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** Inverse-CDF: $X = (1 - (1-U)^{1/b})^{1/a}$. */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return (1.0 - (1.0 - u).pow(1.0 / b)).pow(1.0 / a)
    }

    companion object {
        const val DISTRIBUTION_NAME = "kumaraswamy"

        /**
         * Default: $a = 2.0$, $b = 2.0$ (unimodal, symmetric on $(0,1)$).
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): KumaraswamyGenerator {
            val a = param1 ?: 2.0
            val b = param2 ?: 2.0
            require(a > 0.0) { "Kumaraswamy 'a' (param1) must be strictly positive." }
            require(b > 0.0) { "Kumaraswamy 'b' (param2) must be strictly positive." }
            return KumaraswamyGenerator(a, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class KumaraswamyFactory : DistributionFactory {
    override val name = KumaraswamyGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape a (def. 2.0), param2 = shape b (def. 2.0) — output in (0,1)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        KumaraswamyGenerator.create(p1, p2, commonsRandom)
}
