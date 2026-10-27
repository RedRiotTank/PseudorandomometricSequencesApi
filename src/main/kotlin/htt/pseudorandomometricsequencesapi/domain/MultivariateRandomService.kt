package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateDistributionFactory
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSpec
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSequenceGenerator
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSequenceResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Generates genuinely joint vector-valued sequences.
 *
 * <p>Unlike [RandomService], which samples each variable independently, this service
 * uses multivariate distributions whose components can be statistically dependent
 * (for example, correlated components from a multivariate normal distribution).</p>
 */
@Service
class MultivariateRandomService(factories: List<MultivariateDistributionFactory>) {

    companion object {
        private val logger = LoggerFactory.getLogger(MultivariateRandomService::class.java)

        const val MAX_VALUES_PER_REQUEST = 2_000_000
        private const val MAX_COUNT_MESSAGE = "Count cannot be greater than 2,000,000"
        private const val MAX_TOTAL_VALUES_MESSAGE =
            "Total number of generated values cannot be greater than 2,000,000"
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
        require(count <= MAX_VALUES_PER_REQUEST) { MAX_COUNT_MESSAGE }

        val typeName = RandomSourceFactory.normalizeType(type)
        val seedValue = seed ?: RandomSourceFactory.nextSeed(typeName)
        val distributionName = spec.distribution.lowercase()

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

        require(count.toLong() * generator.dimension <= MAX_VALUES_PER_REQUEST) {
            MAX_TOTAL_VALUES_MESSAGE
        }

        val names = resolveNames(spec.variableNames, generator.dimension)

        logger.info(
            "Built multivariate generator: {}. Distribution: '{}'. Samples: {}. Dimension: {}",
            generator::class.simpleName, distributionName, count, generator.dimension
        )

        val sequence = (1..count).map { generator.sample().toList() }

        return MultivariateSequenceResult(
            distribution = distributionName,
            seed = seedValue,
            variableNames = names,
            sequence = sequence
        )
    }

    private fun resolveNames(provided: List<String>?, dimension: Int): List<String> {
        val names = provided ?: List(dimension) { index -> "x${index + 1}" }

        require(names.size == dimension) {
            "Number of variable names (${names.size}) must match the distribution dimension ($dimension)."
        }
        require(names.all { it.isNotBlank() }) { "Variable names must not be blank." }

        val trimmed = names.map { it.trim() }
        require(trimmed.size == trimmed.toSet().size) { "Variable names must be unique." }

        return trimmed
    }
}
