package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateDistributionFactory
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSpec
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSequenceGenerator
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSequenceResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale

/**
 * Generates genuinely joint vector-valued sequences.
 *
 * <p>Unlike [RandomService], which samples each variable independently, this service
 * uses multivariate distributions whose components can be statistically dependent
 * (for example, correlated components from a multivariate normal distribution).</p>
 */
@Service
class MultivariateRandomService(
    factories: List<MultivariateDistributionFactory>,
    private val limits: ApiLimitsProperties = ApiLimitsProperties(),
    private val concurrency: GenerationConcurrencyLimiter = GenerationConcurrencyLimiter(limits)
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MultivariateRandomService::class.java)
    }

    private val registry: Map<String, MultivariateDistributionFactory> =
        factories.associateBy { it.name }

    fun generate(
        count: Int,
        type: String,
        spec: MultivariateSpec,
        seed: Long? = null
    ): MultivariateSequenceResult {
        require(count > 0) { "Count must be positive." }
        require(count <= limits.maxSamplesPerRequest) { maxCountMessage() }

        rejectOversizedSpec(spec)

        val typeName = RandomSourceFactory.normalizeType(type)
        val seedValue = seed ?: RandomSourceFactory.nextSeed(typeName)
        val distributionName = spec.distribution.lowercase(Locale.ROOT)

        val factory = registry[distributionName]
            ?: throw IllegalArgumentException(
                "Invalid Multivariate Distribution: $distributionName. " +
                    "Use one of the supported types."
            )

        val sources = RandomSourceFactory.create(typeName, seedValue)

        val generator: MultivariateSequenceGenerator = try {
            factory.create(spec, sources.javaRandom, sources.commonsRandom)
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Error creating multivariate generator for '{}'. Message: {}",
                distributionName, e.message
            )
            throw e
        }

        require(count.toLong() * generator.dimension <= limits.maxValuesPerRequest) {
            maxTotalValuesMessage()
        }

        val names = resolveNames(spec.variableNames, generator.dimension)

        logger.info(
            "Built multivariate generator: {}. Distribution: '{}'. Samples: {}. Dimension: {}",
            generator::class.simpleName, distributionName, count, generator.dimension
        )

        val budget = GenerationBudget(limits.maxGenerationNanos, limits.calibrationSamples)
        val plan = budget.plan(
            count,
            "multivariate distribution '$distributionName' with dimension ${generator.dimension}"
        ) { generator.sample() }

        val workUnits = count.toLong() * generator.dimension
        val sequence = concurrency.withPermit(workUnits) {
            plan.finish(budget).map { it.toList() }
        }

        return MultivariateSequenceResult(
            distribution = distributionName,
            seed = seedValue,
            variableNames = names,
            sequence = sequence
        )
    }

    /**
     * Rejects obviously oversized inputs before a generator allocates matrices or lists.
     * The actual dimension is confirmed again after the generator is built.
     */
    private fun rejectOversizedSpec(spec: MultivariateSpec) {
        val requestedDimension = spec.covariance?.size
            ?: spec.concentration?.size
            ?: spec.probabilities?.size
            ?: spec.mean?.size
            ?: spec.variableNames?.size

        require(
            requestedDimension == null ||
                requestedDimension <= limits.maxMultivariateDimension
        ) {
            "Multivariate dimension $requestedDimension exceeds the maximum of " +
                "${limits.maxMultivariateDimension}."
        }

        require(
            spec.variableNames == null ||
                spec.variableNames.size <= limits.maxMultivariateDimension
        ) {
            "Too many variable names: ${spec.variableNames?.size}. The maximum is " +
                "${limits.maxMultivariateDimension}."
        }
    }

    private fun resolveNames(provided: List<String>?, dimension: Int): List<String> {
        val names = provided ?: List(dimension) { index -> "x${index + 1}" }

        require(names.size == dimension) {
            "Number of variable names (${names.size}) must match the distribution dimension ($dimension)."
        }
        require(names.all { it.isNotBlank() }) { "Variable names must not be blank." }
        require(names.all { it.length <= limits.maxVariableNameLength }) {
            "Variable names must not be longer than ${limits.maxVariableNameLength} characters."
        }

        val trimmed = names.map { it.trim() }
        require(trimmed.size == trimmed.toSet().size) { "Variable names must be unique." }

        return trimmed
    }

    private fun maxCountMessage(): String =
        "Count cannot be greater than ${String.format(Locale.ROOT, "%,d", limits.maxSamplesPerRequest)}"

    private fun maxTotalValuesMessage(): String =
        "Total number of generated values cannot be greater than " +
            String.format(Locale.ROOT, "%,d", limits.maxValuesPerRequest)
}
