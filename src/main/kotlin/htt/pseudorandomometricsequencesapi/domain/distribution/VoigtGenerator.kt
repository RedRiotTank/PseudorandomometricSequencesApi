package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.CauchyDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Voigt_profile">Voigt profile</a>}.
 *
 * <p>The convolution of a Gaussian and a Lorentzian (Cauchy), arising when both
 * inhomogeneous (Doppler) and homogeneous (natural/collisional) broadening are present.
 * Widely used in atomic spectroscopy, NMR/ESR lineshape analysis, X-ray diffraction
 * peak fitting, and resonance modelling in HEP (e.g., Z-boson lineshape).</p>
 *
 * <p><strong>Algorithm</strong> — exact by definition as a sum of independent variates:
 * <pre>
 *   X = μ + N(0, σ) + Cauchy(0, γ)
 * </pre></p>
 *
 * @param location Combined location ($\mu$). Default 0.0.
 * @param sigma Gaussian width ($\sigma > 0$). Default 1.0.
 * @param gamma Lorentzian (Cauchy) half-width ($\gamma > 0$). Default 0.5.
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Voigt_profile">Voigt Profile</a>
 */
class VoigtGenerator(
    private val location: Double,
    private val sigma: Double,
    private val gamma: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val normal = NormalDistribution(randomGenerator, 0.0, sigma)
    private val cauchy = CauchyDistribution(randomGenerator, 0.0, gamma)

    override fun sample(): Double = location + normal.sample() + cauchy.sample()

    companion object {
        const val DISTRIBUTION_NAME = "voigt"

        fun create(param1: Double?, param2: Double?, param3: Double?, generator: RandomGenerator): VoigtGenerator {
            val mu    = param1 ?: 0.0
            val sigma = param2 ?: 1.0
            val gamma = param3 ?: 0.5
            require(sigma > 0.0) { "Voigt 'sigma' (param2) must be strictly positive." }
            require(gamma > 0.0) { "Voigt 'gamma' (param3) must be strictly positive." }
            return VoigtGenerator(mu, sigma, gamma, generator)
        }
    }
}

@org.springframework.stereotype.Component
class VoigtFactory : DistributionFactory {
    override val name = VoigtGenerator.DISTRIBUTION_NAME
    override val description = "param1 = location μ (def. 0.0), param2 = Gaussian σ (def. 1.0), param3 = Lorentzian γ (def. 0.5)"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        VoigtGenerator.create(p1, p2, p3, commonsRandom)
}
