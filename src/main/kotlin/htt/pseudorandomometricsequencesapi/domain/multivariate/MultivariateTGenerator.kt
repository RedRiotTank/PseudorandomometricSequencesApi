package htt.pseudorandomometricsequencesapi.domain.multivariate

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.distribution.MultivariateNormalDistribution
import org.apache.commons.math3.exception.MathIllegalArgumentException
import org.apache.commons.math3.random.RandomGenerator
import org.springframework.stereotype.Component
import kotlin.math.sqrt

/**
 * Joint multivariate Student's t distribution.
 *
 * <p>A sample is generated as X = μ + Z·sqrt(ν/U), where Z ~ N(0, Σ) and
 * U ~ ChiSquared(ν). The resulting components share the scale matrix Σ and are
 * therefore correlated whenever Σ has non-zero off-diagonal entries.</p>
 */
class MultivariateTGenerator(
    private val mean: DoubleArray,
    private val covariance: Array<DoubleArray>,
    private val degreesOfFreedom: Double,
    randomGenerator: RandomGenerator
) : MultivariateSequenceGenerator {

    override val dimension: Int = mean.size
    private val normal: MultivariateNormalDistribution
    private val chiSquared: ChiSquaredDistribution

    init {
        require(degreesOfFreedom > 0.0 && degreesOfFreedom.isFinite()) {
            "Multivariate t 'degreesOfFreedom' must be positive and finite."
        }
        validateMeanAndCovariance(mean, covariance, "Multivariate t")
        normal = try {
            MultivariateNormalDistribution(
                randomGenerator,
                DoubleArray(mean.size),
                covariance
            )
        } catch (e: MathIllegalArgumentException) {
            throw IllegalArgumentException(
                "Multivariate t 'covariance' must be positive definite: ${e.message}",
                e
            )
        }
        chiSquared = ChiSquaredDistribution(randomGenerator, degreesOfFreedom)
    }

    override fun sample(): DoubleArray {
        val normalSample = normal.sample()
        var chiSquaredSample = chiSquared.sample()
        var attempts = 0
        while (chiSquaredSample <= 0.0 && attempts < MAX_ATTEMPTS) {
            chiSquaredSample = chiSquared.sample()
            attempts++
        }
        if (chiSquaredSample <= 0.0) {
            throw IllegalStateException("Unable to sample the multivariate t distribution.")
        }

        val scale = sqrt(degreesOfFreedom / chiSquaredSample)

        return DoubleArray(dimension) { index ->
            mean[index] + normalSample[index] * scale
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 100
    }
}

@Component
class MultivariateTFactory : MultivariateDistributionFactory {

    override val name = "multivariate-t"

    override val description =
        "mean = location vector μ (default zeros), covariance = scale matrix Σ, degreesOfFreedom = ν > 0"

    override fun create(
        spec: MultivariateSpec,
        javaRandom: java.util.Random,
        commonsRandom: RandomGenerator
    ): MultivariateSequenceGenerator {
        val covariance = spec.covariance
            ?: throw IllegalArgumentException("Multivariate t requires 'covariance'.")
        val degreesOfFreedom = spec.degreesOfFreedom
            ?: throw IllegalArgumentException("Multivariate t requires 'degreesOfFreedom'.")
        val mean = spec.mean?.toDoubleArray() ?: DoubleArray(covariance.size)

        return MultivariateTGenerator(
            mean = mean,
            covariance = covariance.map { it.toDoubleArray() }.toTypedArray(),
            degreesOfFreedom = degreesOfFreedom,
            randomGenerator = commonsRandom
        )
    }
}
