package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
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
import java.util.Locale
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
class CopulaRandomService(
    factories: List<DistributionFactory>,
    private val limits: ApiLimitsProperties = ApiLimitsProperties(),
    private val concurrency: GenerationConcurrencyLimiter = GenerationConcurrencyLimiter(limits)
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CopulaRandomService::class.java)
        private const val DEADLINE_CHECK_MASK = 0xFFF
    }

    private val registry: Map<String, DistributionFactory> = factories.associateBy { it.name }

    fun generate(
        count: Int,
        type: String,
        variables: List<CopulaVariableSpec>,
        correlation: List<List<Double>>,
        seed: Long? = null,
        quantileSamples: Int? = null
    ): CopulaResult {
        val effectiveQuantileSamples = quantileSamples ?: limits.defaultQuantileSamples

        require(count > 0) { "Count must be positive." }
        require(count <= limits.maxSamplesPerRequest) { maxSamplesMessage() }
        require(variables.isNotEmpty()) { "At least one variable must be specified." }
        require(variables.size <= limits.maxVariablesPerRequest) {
            "At most ${limits.maxVariablesPerRequest} variables can be generated in one request."
        }
        require(count.toLong() * variables.size <= limits.maxValuesPerRequest) {
            maxTotalSamplesMessage()
        }
        require(effectiveQuantileSamples in limits.minQuantileSamples..limits.maxQuantileSamples) {
            "quantileSamples must be between ${formatLimit(limits.minQuantileSamples)} and " +
                "${formatLimit(limits.maxQuantileSamples)}."
        }
        require(effectiveQuantileSamples.toLong() * variables.size <= limits.maxTotalQuantileSamples) {
            "Total quantile samples across all variables cannot be greater than " +
                "${formatLimit(limits.maxTotalQuantileSamples)}."
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

        val workUnits =
            effectiveQuantileSamples.toLong() * variables.size + count.toLong() * variables.size

        val sequences = concurrency.withPermit(workUnits) {
            val budget = GenerationBudget(limits.maxGenerationNanos, limits.calibrationSamples)
            val quantileTables = variables.mapIndexed { index, spec ->
                buildQuantileTable(
                    spec = spec,
                    typeName = typeName,
                    seed = quantileSeeds[index],
                    quantileSamples = effectiveQuantileSamples,
                    budget = budget
                )
            }

            val standardNormal = NormalDistribution(0.0, 1.0)
            val plan = budget.plan(
                count,
                "Gaussian copula with ${variables.size} variables"
            ) {
                val latent = latentDistribution.sample()
                DoubleArray(variables.size) { variableIndex ->
                    val uniform = standardNormal.cumulativeProbability(latent[variableIndex])
                    quantile(quantileTables[variableIndex], uniform)
                }
            }

            val observations = plan.finish(budget)
            List(variables.size) { variableIndex ->
                DoubleArray(count) { observation -> observations[observation][variableIndex] }
                    .toList()
            }
        }

        logger.info(
            "Generated Gaussian-copula sequence. Variables: {}, Samples: {}, Correlation dimension: {}, " +
                "Quantile samples: {}",
            variables.size, count, variables.size, effectiveQuantileSamples
        )

        return CopulaResult(
            seed = masterSeed,
            correlation = correlationMatrix.map { row -> row.toList() },
            quantileSamples = effectiveQuantileSamples,
            variables = variables.mapIndexed { index, spec ->
                CopulaVariableSequence(
                    name = names[index],
                    distribution = spec.distribution.lowercase(Locale.ROOT),
                    sequence = sequences[index]
                )
            }
        )
    }

    private fun buildQuantileTable(
        spec: CopulaVariableSpec,
        typeName: String,
        seed: Long,
        quantileSamples: Int,
        budget: GenerationBudget
    ): DoubleArray {
        val distributionName = spec.distribution.lowercase(Locale.ROOT)
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

        val table = DoubleArray(quantileSamples)
        for (index in 0 until quantileSamples) {
            table[index] = generator.sample()
            if (index and DEADLINE_CHECK_MASK == 0) {
                budget.ensureWithinDeadline("the quantile table for '$distributionName'")
            }
        }
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
            require(provided == null || provided.length <= limits.maxVariableNameLength) {
                "Variable names must not be longer than ${limits.maxVariableNameLength} characters."
            }
            provided?.trim() ?: "variable${index + 1}"
        }
        require(names.size == names.toSet().size) { "Variable names must be unique." }
        return names
    }

    private fun maxSamplesMessage(): String =
        "Count cannot be greater than ${formatLimit(limits.maxSamplesPerRequest)}"

    private fun maxTotalSamplesMessage(): String =
        "Total samples across all variables cannot be greater than " +
            formatLimit(limits.maxValuesPerRequest)

    private fun formatLimit(value: Long): String = String.format(Locale.ROOT, "%,d", value)

    private fun formatLimit(value: Int): String = formatLimit(value.toLong())
}
