package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.copula.CopulaResult
import htt.pseudorandomometricsequencesapi.domain.copula.CopulaVariableSequence
import htt.pseudorandomometricsequencesapi.domain.copula.CopulaVariableSpec
import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import org.apache.commons.math3.distribution.MultivariateNormalDistribution
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.exception.MathIllegalArgumentException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Random
import kotlin.math.abs

/**
 * Generates statistically dependent variables using a Gaussian copula.
 *
 * <p>The copula is built in three steps:</p>
 * <ol>
 *   <li>Draw a latent vector Z from a multivariate normal distribution with the
 *       requested correlation matrix.</li>
 *   <li>Convert each component to a uniform value with the standard normal CDF:
 *       U_i = Φ(Z_i).</li>
 *   <li>Map each uniform value through the inverse CDF of the requested marginal
 *       distribution: X_i = F_i⁻¹(U_i).</li>
 * </ol>
 *
 * <p>Because the project supports many custom distributions, inverse CDFs are
 * represented by empirical quantile tables generated from each marginal's existing
 * generator. Increasing `quantileSamples` improves the marginal approximation,
 * especially in the tails.</p>
 */
@Service
class CopulaRandomService(factories: List<DistributionFactory>) {

    companion object {
        private val logger = LoggerFactory.getLogger(CopulaRandomService::class.java)

        const val MAX_SAMPLES_PER_REQUEST = 2_000_000
        const val DEFAULT_QUANTILE_SAMPLES = 100_000
        const val MIN_QUANTILE_SAMPLES = 10_000
        const val MAX_QUANTILE_SAMPLES = 1_000_000
        const val MAX_VARIABLES = 100
        const val MAX_TOTAL_QUANTILE_SAMPLES = 20_000_000

        private const val MAX_SAMPLES_MESSAGE = "Count cannot be greater than 2,000,000"
        private const val MAX_TOTAL_SAMPLES_MESSAGE =
            "Total samples across all variables cannot be greater than 2,000,000"
    }

    private val registry: Map<String, DistributionFactory> = factories.associateBy { it.name }

    fun generate(
        count: Int,
        type: String,
        variables: List<CopulaVariableSpec>,
        correlation: List<List<Double>>,
        seed: Long? = null,
        quantileSamples: Int = DEFAULT_QUANTILE_SAMPLES
    ): CopulaResult {
        require(count > 0) { "Count must be positive." }
        require(count <= MAX_SAMPLES_PER_REQUEST) { MAX_SAMPLES_MESSAGE }
        require(variables.isNotEmpty()) { "At least one variable must be specified." }
        require(variables.size <= MAX_VARIABLES) {
            "At most $MAX_VARIABLES variables can be generated in one request."
        }
        require(count.toLong() * variables.size <= MAX_SAMPLES_PER_REQUEST) {
            MAX_TOTAL_SAMPLES_MESSAGE
        }
        require(quantileSamples in MIN_QUANTILE_SAMPLES..MAX_QUANTILE_SAMPLES) {
            "quantileSamples must be between %,d and %,d.".format(
                MIN_QUANTILE_SAMPLES,
                MAX_QUANTILE_SAMPLES
            )
        }
        require(quantileSamples.toLong() * variables.size <= MAX_TOTAL_QUANTILE_SAMPLES) {
            "Total quantile samples across all variables cannot be greater than %,d.".format(
                MAX_TOTAL_QUANTILE_SAMPLES
            )
        }

        val typeName = RandomSourceFactory.normalizeType(type)
        val masterSeed = seed ?: RandomSourceFactory.nextSeed(typeName)
        val names = resolveNames(variables)
        val correlationMatrix = validateCorrelation(correlation, variables.size)

        val seedSource = Random(masterSeed)
        val latentSeed = seedSource.nextLong()
        val quantileSeeds = LongArray(variables.size) { seedSource.nextLong() }

        val latentSources = RandomSourceFactory.create(typeName, latentSeed)
        val latentDistribution = try {
            MultivariateNormalDistribution(
                latentSources.commonsRandom,
                DoubleArray(variables.size),
                correlationMatrix
            )
        } catch (e: MathIllegalArgumentException) {
            throw IllegalArgumentException(
                "Correlation matrix must be positive definite: ${e.message}",
                e
            )
        }

        val quantileTables = variables.mapIndexed { index, spec ->
            buildQuantileTable(
                spec = spec,
                typeName = typeName,
                seed = quantileSeeds[index],
                quantileSamples = quantileSamples
            )
        }

        val standardNormal = NormalDistribution(0.0, 1.0)
        val sequences = Array(variables.size) { DoubleArray(count) }

        repeat(count) { observation ->
            val latent = latentDistribution.sample()
            for (variableIndex in variables.indices) {
                val uniform = standardNormal.cumulativeProbability(latent[variableIndex])
                sequences[variableIndex][observation] =
                    quantile(quantileTables[variableIndex], uniform)
            }
        }

        logger.info(
            "Generated Gaussian-copula sequence. Variables: {}, Samples: {}, Correlation dimension: {}, " +
                "Quantile samples: {}",
            variables.size, count, variables.size, quantileSamples
        )

        return CopulaResult(
            seed = masterSeed,
            correlation = correlationMatrix.map { row -> row.toList() },
            quantileSamples = quantileSamples,
            variables = variables.mapIndexed { index, spec ->
                CopulaVariableSequence(
                    name = names[index],
                    distribution = spec.distribution.lowercase(),
                    sequence = sequences[index].toList()
                )
            }
        )
    }

    private fun buildQuantileTable(
        spec: CopulaVariableSpec,
        typeName: String,
        seed: Long,
        quantileSamples: Int
    ): DoubleArray {
        val distributionName = spec.distribution.lowercase()
        val factory = registry[distributionName]
            ?: throw IllegalArgumentException(
                "Invalid Distribution: $distributionName. Use one of the supported types."
            )

        val sources = RandomSourceFactory.create(typeName, seed)
        val generator: SequenceGenerator = try {
            factory.create(spec.param1, spec.param2, spec.param3, sources.javaRandom, sources.commonsRandom)
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Error creating copula marginal '{}' with Params: [{}, {}, {}]. Message: {}",
                distributionName, spec.param1, spec.param2, spec.param3, e.message
            )
            throw e
        }

        val table = DoubleArray(quantileSamples) { generator.sample() }
        table.sort()
        return table
    }

    private fun quantile(sortedTable: DoubleArray, uniform: Double): Double {
        val clamped = uniform.coerceIn(0.0, 1.0)
        val index = (clamped * (sortedTable.size - 1)).toInt()
        return sortedTable[index]
    }

    private fun validateCorrelation(
        correlation: List<List<Double>>,
        dimension: Int
    ): Array<DoubleArray> {
        require(correlation.size == dimension) {
            "Correlation matrix must have $dimension rows (one per variable)."
        }

        val matrix = Array(dimension) { rowIndex ->
            val row = correlation[rowIndex]
            require(row.size == dimension) {
                "Correlation matrix must be a square ${dimension}x${dimension} matrix."
            }
            row.toDoubleArray()
        }

        for (i in 0 until dimension) {
            require(abs(matrix[i][i] - 1.0) <= 1e-9) {
                "Correlation matrix diagonal must be 1.0."
            }
            for (j in 0 until dimension) {
                require(matrix[i][j].isFinite()) {
                    "Correlation matrix must contain only finite values."
                }
                require(abs(matrix[i][j]) <= 1.0) {
                    "Correlation matrix entries must be between -1 and 1."
                }
                require(abs(matrix[i][j] - matrix[j][i]) <= 1e-9) {
                    "Correlation matrix must be symmetric."
                }
            }
        }

        return matrix
    }

    private fun resolveNames(variables: List<CopulaVariableSpec>): List<String> {
        val names = variables.mapIndexed { index, spec ->
            val provided = spec.name
            require(provided == null || provided.isNotBlank()) { "Variable names must not be blank." }
            provided?.trim() ?: "variable${index + 1}"
        }
        require(names.size == names.toSet().size) { "Variable names must be unique." }
        return names
    }
}
