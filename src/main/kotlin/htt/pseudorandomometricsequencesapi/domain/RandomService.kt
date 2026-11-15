package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.Random

/**
 * Generates pseudorandom sequences for one or more independent univariate variables.
 *
 * <p>Variables are generated independently: each one receives its own random number
 * generators and its own seed. When a request-level seed is supplied, the per-variable
 * seeds are derived deterministically from it, so the same request always produces the
 * same result. A variable may also specify its own seed, which makes that variable
 * reproducible in isolation.</p>
 *
 * <p>For genuinely joint (correlated) vector-valued generation, see
 * [MultivariateRandomService].</p>
 */
@Service
class RandomService(
    factories: List<DistributionFactory>,
    private val limits: ApiLimitsProperties = ApiLimitsProperties(),
    private val concurrency: GenerationConcurrencyLimiter = GenerationConcurrencyLimiter(limits)
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RandomService::class.java)
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

        val typeName = RandomSourceFactory.normalizeType(type)
        val seedValue = seed ?: RandomSourceFactory.nextSeed(typeName)

        val prepared = prepareVariable(
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

        val budget = newBudget()
        val plan = budget.plan(count, labelFor(prepared), prepared.generator::sample)
        val sequence = concurrency.withPermit(count.toLong()) { plan.finish(budget) }

        return SequenceResult(sequence, seedValue)
    }

    /**
     * Generates one independent sequence per requested variable.
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
        require(variables.size <= limits.maxVariablesPerRequest) {
            "Too many variables: ${variables.size}. The maximum is ${limits.maxVariablesPerRequest}."
        }
        require(count.toLong() * variables.size <= limits.maxValuesPerRequest) {
            maxTotalSamplesMessage()
        }

        val typeName = RandomSourceFactory.normalizeType(type)
        val masterSeed = seed ?: RandomSourceFactory.nextSeed(typeName)
        val names = resolveNames(variables)

        // Always derive one seed per variable, even when the variable brings its own
        // seed. This keeps derived seeds stable if another variable is given an
        // explicit seed later.
        val seedSource = Random(masterSeed)
        val derivedSeeds = LongArray(variables.size) { seedSource.nextLong() }

        // Build every generator before sampling so limit and parameter errors fail fast.
        val prepared = variables.mapIndexed { index, spec ->
            val variableSeed = spec.seed ?: derivedSeeds[index]
            prepareVariable(
                typeName = typeName,
                spec = spec.copy(name = names[index]),
                seed = variableSeed
            )
        }

        val budget = newBudget()
        val plans = prepared.map { budget.plan(count, labelFor(it), it.generator::sample) }

        val results = concurrency.withPermit(count.toLong() * variables.size) {
            prepared.mapIndexed { index, variable ->
                VariableSequence(
                    name = variable.spec.name ?: "variable",
                    distribution = variable.distributionName,
                    seed = variable.seed,
                    sequence = plans[index].finish(budget)
                )
            }
        }

        return MultiSequenceResult(results, masterSeed)
    }

    private fun prepareVariable(
        typeName: String,
        spec: VariableSpec,
        seed: Long
    ): PreparedVariable {
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
                "Error creating generator for '{}' with Params: [{}, {}, {}]. Message: {}",
                distributionName, spec.param1, spec.param2, spec.param3, e.message
            )
            throw e
        }

        logger.info(
            "Built generator: {}. Variable: '{}'. Distribution: '{}'",
            generator::class.simpleName, spec.name, distributionName
        )

        return PreparedVariable(spec, distributionName, seed, generator)
    }

    private fun labelFor(variable: PreparedVariable): String =
        "variable '${variable.spec.name ?: "variable"}' using distribution '${variable.distributionName}'"

    private fun newBudget(): GenerationBudget =
        GenerationBudget(limits.maxGenerationNanos, limits.calibrationSamples)

    private fun validateCount(count: Int) {
        require(count > 0) { "Count must be positive." }
        require(count <= limits.maxSamplesPerRequest) { maxSamplesMessage() }
        logger.debug("Count validation successful: {}", count)
    }

    private fun maxSamplesMessage(): String =
        "Count cannot be greater than ${formatLimit(limits.maxSamplesPerRequest)}"

    private fun maxTotalSamplesMessage(): String =
        "Total samples across all variables cannot be greater than " +
            formatLimit(limits.maxValuesPerRequest)

    private fun formatLimit(value: Int): String = String.format(Locale.ROOT, "%,d", value)

    private fun resolveNames(variables: List<VariableSpec>): List<String> {
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

    private data class PreparedVariable(
        val spec: VariableSpec,
        val distributionName: String,
        val seed: Long,
        val generator: SequenceGenerator
    )
}
