package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

/**
 * Generates pseudorandom sequences for one or more variables.
 *
 * <p>Variables are generated independently: each one receives its own random number
 * generators and its own seed. When a request-level seed is supplied, the per-variable
 * seeds are derived deterministically from it, so the same request always produces the
 * same result. A variable may also specify its own seed, which makes that variable
 * reproducible in isolation.</p>
 */
@Service
class RandomService(factories: List<DistributionFactory>) {

    companion object {
        private val logger = LoggerFactory.getLogger(RandomService::class.java)
        private val SECURE_RANDOM_INSTANCE by lazy { SecureRandom() }

        const val MAX_SAMPLES_PER_REQUEST = 2_000_000
        private const val MAX_SAMPLES_MESSAGE = "Count cannot be greater than 2,000,000"
        private const val MAX_TOTAL_SAMPLES_MESSAGE =
            "Total samples across all variables cannot be greater than 2,000,000"
        private const val INVALID_TYPE_MESSAGE = "Invalid type. Use 'secure' or 'general'."
    }

    private val registry: Map<String, DistributionFactory> = factories.associateBy { it.name }

    /**
     * Generates a single sequence, preserving the original single-variable API.
     */
    fun generateSequence(
        count: Int,
        type: String,
        distribution: String,
        param1: Double? = null,
        param2: Double? = null,
        param3: Double? = null,
        seed: Long? = null
    ): SequenceResult {
        validateCount(count)

        val typeName = normalizeType(type)
        val seedValue = seed ?: nextSeed(typeName)

        val variable = generateVariable(
            count = count,
            typeName = typeName,
            spec = VariableSpec(
                name = "variable1",
                distribution = distribution,
                param1 = param1,
                param2 = param2,
                param3 = param3
            ),
            seed = seedValue
        )

        return SequenceResult(variable.sequence, seedValue)
    }

    /**
     * Generates one sequence per requested variable.
     *
     * @param count Number of samples to generate for each variable.
     * @param type Generator type (`general` or `secure`).
     * @param variables Variable definitions. Must not be empty and names must be unique.
     * @param seed Optional request-level seed used to derive per-variable seeds.
     * @return The generated sequences and their per-variable seeds.
     */
    fun generateSequences(
        count: Int,
        type: String,
        variables: List<VariableSpec>,
        seed: Long? = null
    ): MultiSequenceResult {
        validateCount(count)
        require(variables.isNotEmpty()) { "At least one variable must be specified." }
        require(count.toLong() * variables.size <= MAX_SAMPLES_PER_REQUEST) {
            MAX_TOTAL_SAMPLES_MESSAGE
        }

        val typeName = normalizeType(type)
        val masterSeed = seed ?: nextSeed(typeName)
        val names = resolveNames(variables)

        // Always derive one seed per variable, even when the variable brings its own
        // seed. This keeps derived seeds stable if another variable is given an
        // explicit seed later.
        val seedSource = Random(masterSeed)
        val derivedSeeds = LongArray(variables.size) { seedSource.nextLong() }

        val results = variables.mapIndexed { index, spec ->
            val variableSeed = spec.seed ?: derivedSeeds[index]
            generateVariable(
                count = count,
                typeName = typeName,
                spec = spec.copy(name = names[index]),
                seed = variableSeed
            )
        }

        return MultiSequenceResult(results, masterSeed)
    }

    private fun generateVariable(
        count: Int,
        typeName: String,
        spec: VariableSpec,
        seed: Long
    ): VariableSequence {
        val distributionName = spec.distribution.lowercase()

        val factory = registry[distributionName]
            ?: throw IllegalArgumentException(
                "Invalid Distribution: $distributionName. Use one of the supported types."
            )

        val (javaRandom, commonsRandom) = createRandomGenerators(typeName, seed)

        val generator: SequenceGenerator = try {
            factory.create(spec.param1, spec.param2, spec.param3, javaRandom, commonsRandom)
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Error creating generator for '{}' with Params: [{}, {}, {}]. Message: {}",
                distributionName, spec.param1, spec.param2, spec.param3, e.message
            )
            throw e
        }

        logger.info(
            "Built generator: {}. Variable: '{}'. Samples to generate: {}",
            generator::class.simpleName, spec.name, count
        )

        val sequence = (1..count).map { generator.sample() }
        return VariableSequence(
            name = spec.name ?: "variable",
            distribution = distributionName,
            seed = seed,
            sequence = sequence
        )
    }

    private fun createRandomGenerators(typeName: String, seed: Long): Pair<Random, RandomGenerator> {
        val javaRandom: Random = when (typeName) {
            "secure" -> {
                logger.debug("Using generator type: SecureRandom with seed: {}", seed)
                try {
                    SecureRandom.getInstance("SHA1PRNG").apply { setSeed(seed) }
                } catch (e: Exception) {
                    logger.warn(
                        "SHA1PRNG SecureRandom not available, falling back to java.util.Random: {}",
                        e.message
                    )
                    Random(seed)
                }
            }

            "general" -> {
                logger.debug("Using generator type: Random (general) with seed: {}", seed)
                Random(seed)
            }

            else -> throw IllegalArgumentException(INVALID_TYPE_MESSAGE)
        }

        val commonsRandom = JDKRandomGenerator().apply { setSeed(seed) }
        logger.debug("Apache Commons Math generator seed: {}", seed)

        return javaRandom to commonsRandom
    }

    private fun validateCount(count: Int) {
        require(count > 0) { "Count must be positive." }
        require(count <= MAX_SAMPLES_PER_REQUEST) { MAX_SAMPLES_MESSAGE }
        logger.debug("Count validation successful: {}", count)
    }

    private fun normalizeType(type: String): String {
        val typeName = type.lowercase()
        if (typeName != "general" && typeName != "secure") {
            logger.error("Invalid generator type: {}", typeName)
            throw IllegalArgumentException(INVALID_TYPE_MESSAGE)
        }
        return typeName
    }

    private fun nextSeed(typeName: String): Long = when (typeName) {
        "secure" -> SECURE_RANDOM_INSTANCE.nextLong()
        "general" -> ThreadLocalRandom.current().nextLong()
        else -> throw IllegalArgumentException(INVALID_TYPE_MESSAGE)
    }

    private fun resolveNames(variables: List<VariableSpec>): List<String> {
        val names = variables.mapIndexed { index, spec ->
            val provided = spec.name
            require(provided == null || provided.isNotBlank()) { "Variable names must not be blank." }
            provided?.trim() ?: "variable${index + 1}"
        }
        require(names.size == names.toSet().size) { "Variable names must be unique." }
        return names
    }
}
