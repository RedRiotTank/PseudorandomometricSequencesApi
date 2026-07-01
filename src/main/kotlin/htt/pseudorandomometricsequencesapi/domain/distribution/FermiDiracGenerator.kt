package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.exp

/**
 * Represents a generator for fermion occupation numbers following
 * {@link <a href="https://en.wikipedia.org/wiki/Fermi%E2%80%93Dirac_statistics">Fermi–Dirac statistics</a>}.
 *
 * <p>A mode of energy $\varepsilon$ is occupied ($n=1$) with probability $f(\varepsilon)$ or
 * empty ($n=0$) with probability $1 - f(\varepsilon)$, where $f(\varepsilon)$ is the Fermi–Dirac
 * distribution function. Models electron/hole occupation in semiconductors, neutron distribution
 * in neutron stars, and quark Pauli blocking in heavy-ion collisions.</p>
 *
 * <p>$P(n=1) = f = 1 / (\exp(\varepsilon/kT) + 1)$.</p>
 *
 * <p>Returns 0 or 1 only (Pauli exclusion principle). Equivalent to
 * Bernoulli$(1/(\exp(\varepsilon/kT)+1))$.</p>
 *
 * @param epsilonOverKT Reduced energy $\varepsilon/kT$ (can be negative for degenerate Fermi gas). Default 1.0.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Fermi%E2%80%93Dirac_statistics">Fermi–Dirac Statistics</a>
 */
class FermiDiracGenerator(
    epsilonOverKT: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val pOccupied: Double = 1.0 / (1.0 + exp(epsilonOverKT))

    override fun sample(): Double = if (randomGenerator.nextDouble() < pOccupied) 1.0 else 0.0

    companion object {
        const val DISTRIBUTION_NAME = "fermi-dirac"

        fun create(param1: Double?, generator: RandomGenerator): FermiDiracGenerator {
            val ekT = param1 ?: 1.0
            return FermiDiracGenerator(ekT, generator)
        }
    }
}

@org.springframework.stereotype.Component
class FermiDiracFactory : DistributionFactory {
    override val name = FermiDiracGenerator.DISTRIBUTION_NAME
    override val description = "param1 = ε/kT (def. 1.0; can be negative) — returns 0 or 1; P(1) = 1/(exp(ε/kT)+1)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        FermiDiracGenerator.create(p1, commonsRandom)
}
