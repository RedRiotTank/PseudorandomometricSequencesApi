package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.GeometricDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.exp

/**
 * Represents a generator for photon occupation numbers following
 * {@link <a href="https://en.wikipedia.org/wiki/Bose%E2%80%93Einstein_statistics">Bose–Einstein statistics</a>}.
 *
 * <p>The occupation number $n$ of a bosonic mode with energy $\varepsilon$ at temperature $T$
 * follows a Geometric distribution: $P(n) = (1-p)\,p^n$ for $n = 0, 1, 2, \ldots$
 * where $p = \exp(-\varepsilon/kT)$. The mean occupation is the Planck factor
 * $\bar{n} = 1/(\exp(\varepsilon/kT) - 1)$.</p>
 *
 * <p>Returns non-negative integers. Used in quantum optics, phonon statistics,
 * and detector photon-count modelling.</p>
 *
 * @param epsilonOverKT Reduced energy $\varepsilon / kT > 0$. Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bose%E2%80%93Einstein_statistics">Bose–Einstein Statistics</a>
 */
class BoseEinsteinGenerator(
    epsilonOverKT: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    // P(n) = (1-p)*p^n with p = exp(-ε/kT)
    private val geometric = GeometricDistribution(randomGenerator, 1.0 - exp(-epsilonOverKT))

    override fun sample(): Double = geometric.sample().toDouble()

    companion object {
        const val DISTRIBUTION_NAME = "bose-einstein"

        fun create(param1: Double?, generator: RandomGenerator): BoseEinsteinGenerator {
            val ekT = param1 ?: 1.0
            require(ekT > 0.0) { "Bose-Einstein 'epsilon/kT' (param1) must be strictly positive." }
            return BoseEinsteinGenerator(ekT, generator)
        }
    }
}

@org.springframework.stereotype.Component
class BoseEinsteinFactory : DistributionFactory {
    override val name = BoseEinsteinGenerator.DISTRIBUTION_NAME
    override val description = "param1 = ε/kT > 0 (def. 1.0) — boson occupation number; mean = 1/(exp(ε/kT) − 1)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        BoseEinsteinGenerator.create(p1, commonsRandom)
}
