package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Conway%E2%80%93Maxwell%E2%80%93Poisson_distribution">Conway–Maxwell–Poisson distribution</a>}
 * (CMP).
 *
 * <p>A flexible generalisation of the Poisson distribution that can model both
 * over-dispersed ($\nu < 1$) and under-dispersed ($\nu > 1$) count data. Special cases:
 * $\nu = 1$ is Poisson($\lambda$), $\nu = 0$ is Geometric, $\nu \to \infty$ is Bernoulli.</p>
 *
 * <p>PMF: $P(X = k) \propto \frac{\lambda^k}{(k!)^\nu}$, with recurrence
 * $P(X = k+1) = P(X = k) \cdot \lambda / (k+1)^\nu$.</p>
 *
 * <p><strong>Algorithm</strong> — truncated CDF inversion using the recurrence. Probabilities
 * are pre-computed at construction until they become negligible ($< 10^{-14}$ of the running
 * sum), then normalised. Sampling uses binary search over the CDF.</p>
 *
 * @param lambda Rate parameter ($\lambda > 0$). Default 2.0.
 * @param nu Dispersion parameter ($\nu \ge 0$). Default 1.0 (Poisson).
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Conway%E2%80%93Maxwell%E2%80%93Poisson_distribution">CMP Distribution</a>
 */
class ConwayMaxwellPoissonGenerator(
    private val lambda: Double,
    private val nu: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val cdf: DoubleArray

    init {
        val maxK = 1000
        val unnorm = DoubleArray(maxK + 1)
        unnorm[0] = 1.0
        var sum = 1.0
        var cutoff = maxK
        for (k in 1..maxK) {
            unnorm[k] = unnorm[k - 1] * lambda / k.toDouble().pow(nu)
            sum += unnorm[k]
            if (unnorm[k] < 1e-14 * sum) {
                cutoff = k
                break
            }
        }
        val temp = DoubleArray(cutoff + 1)
        var cumul = 0.0
        for (k in 0 until cutoff) {
            cumul += unnorm[k] / sum
            temp[k] = cumul
        }
        temp[cutoff] = 1.0
        cdf = temp
    }

    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        var lo = 0; var hi = cdf.size - 1
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (cdf[mid] < u) lo = mid + 1 else hi = mid
        }
        return lo.toDouble()
    }

    companion object {
        const val DISTRIBUTION_NAME = "conway-maxwell-poisson"

        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ConwayMaxwellPoissonGenerator {
            val lambda = param1 ?: 2.0
            val nu = param2 ?: 1.0
            require(lambda > 0.0) { "Conway-Maxwell-Poisson 'lambda' (param1) must be strictly positive." }
            require(nu >= 0.0) { "Conway-Maxwell-Poisson 'nu' (param2) must be non-negative." }
            return ConwayMaxwellPoissonGenerator(lambda, nu, generator)
        }
    }
}

@org.springframework.stereotype.Component
class ConwayMaxwellPoissonFactory : DistributionFactory {
    override val name = ConwayMaxwellPoissonGenerator.DISTRIBUTION_NAME
    override val description = "param1 = rate λ (def. 2.0), param2 = dispersion ν (def. 1.0; ν=1=Poisson, ν<1=overdispersed, ν>1=underdispersed)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        ConwayMaxwellPoissonGenerator.create(p1, p2, commonsRandom)
}
