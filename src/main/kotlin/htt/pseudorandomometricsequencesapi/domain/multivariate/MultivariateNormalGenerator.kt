package htt.pseudorandomometricsequencesapi.domain.multivariate

import org.apache.commons.math3.distribution.MultivariateNormalDistribution
import org.apache.commons.math3.exception.MathIllegalArgumentException
import org.apache.commons.math3.random.RandomGenerator
import org.springframework.stereotype.Component
import kotlin.math.abs

internal fun validateMeanAndCovariance(
    mean: DoubleArray,
    covariance: Array<DoubleArray>,
    label: String
) {
    require(mean.isNotEmpty()) { "$label 'mean' must not be empty." }
    require(covariance.size == mean.size) {
        "$label 'covariance' must have ${mean.size} rows."
    }
    require(covariance.all { it.size == mean.size }) {
        "$label 'covariance' must be a square ${mean.size}x${mean.size} matrix."
    }
    require(mean.all { it.isFinite() }) {
        "$label 'mean' must contain only finite values."
    }
    require(covariance.all { row -> row.all { it.isFinite() } }) {
        "$label 'covariance' must contain only finite values."
    }
    for (i in covariance.indices) {
        for (j in covariance.indices) {
            require(abs(covariance[i][j] - covariance[j][i]) <= 1e-9) {
                "$label 'covariance' must be symmetric."
            }
        }
    }
}

/**
 * Joint multivariate normal distribution.
 *
 * <p>Samples are drawn from N(μ, Σ), where Σ is the supplied covariance matrix. The
 * covariance matrix may contain non-zero off-diagonal entries, in which case the
 * generated components are correlated.</p>
 */
class MultivariateNormalGenerator(
    private val mean: DoubleArray,
    private val covariance: Array<DoubleArray>,
    randomGenerator: RandomGenerator
) : MultivariateSequenceGenerator {

    override val dimension: Int = mean.size
    private val distribution: MultivariateNormalDistribution

    init {
        validateMeanAndCovariance(mean, covariance, "Multivariate normal")
        distribution = try {
            MultivariateNormalDistribution(randomGenerator, mean, covariance)
        } catch (e: MathIllegalArgumentException) {
            throw IllegalArgumentException(
                "Multivariate normal 'covariance' must be positive definite: ${e.message}",
                e
            )
        }
    }

    override fun sample(): DoubleArray = distribution.sample()
}

@Component
class MultivariateNormalFactory : MultivariateDistributionFactory {

    override val name = "multivariate-normal"

    override val description =
        "mean = mean vector μ (default zeros), covariance = symmetric positive-definite covariance matrix Σ"

    override fun create(
        spec: MultivariateSpec,
        javaRandom: java.util.Random,
        commonsRandom: RandomGenerator
    ): MultivariateSequenceGenerator {
        val covariance = spec.covariance
            ?: throw IllegalArgumentException("Multivariate normal requires 'covariance'.")
        val mean = spec.mean?.toDoubleArray() ?: DoubleArray(covariance.size)

        return MultivariateNormalGenerator(
            mean = mean,
            covariance = covariance.map { it.toDoubleArray() }.toTypedArray(),
            randomGenerator = commonsRandom
        )
    }
}
