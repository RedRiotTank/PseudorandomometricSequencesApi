package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.LaplaceDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.exp

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Log-Laplace_distribution">Log-Laplace distribution</a>}.
 *
 * <p>A random variable $X$ is Log-Laplace distributed if $\ln X$ follows a Laplace distribution.
 * It has heavier tails than the Log-Normal and is used in financial modelling (extreme return
 * distributions), internet traffic analysis, and Bayesian compressed sensing.</p>
 *
 * <p><strong>Algorithm</strong>: $X = \exp(\text{Laplace}(\mu, b))$.</p>
 *
 * @param logLocation Log-location parameter ($\mu$). Default 0.0.
 * @param logScale Log-scale parameter ($b > 0$). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Log-Laplace_distribution">Log-Laplace Distribution</a>
 */
class LogLaplaceGenerator(
    logLocation: Double,
    logScale: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val laplace = LaplaceDistribution(randomGenerator, logLocation, logScale)

    override fun sample(): Double = exp(laplace.sample())

    companion object {
        const val DISTRIBUTION_NAME = "log-laplace"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): LogLaplaceGenerator {
            val mu = param1 ?: 0.0
            val b  = param2 ?: 1.0
            require(b > 0.0) { "Log-Laplace 'logScale' (param2) must be strictly positive." }
            return LogLaplaceGenerator(mu, b, generator)
        }
    }
}

@org.springframework.stereotype.Component
class LogLaplaceFactory : DistributionFactory {
    override val name = LogLaplaceGenerator.DISTRIBUTION_NAME
    override val description = "param1 = log-location μ (def. 0.0), param2 = log-scale b (def. 1.0)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        LogLaplaceGenerator.create(p1, p2, commonsRandom)
}
