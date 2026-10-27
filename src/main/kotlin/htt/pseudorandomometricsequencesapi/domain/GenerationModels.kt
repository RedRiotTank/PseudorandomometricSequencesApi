package htt.pseudorandomometricsequencesapi.domain

/**
 * Description of a single variable to generate.
 *
 * @property name Optional human-readable name. When omitted, the service assigns
 * `variable1`, `variable2`, ...
 * @property distribution The distribution name (case-insensitive).
 * @property param1 First distribution parameter.
 * @property param2 Second distribution parameter.
 * @property param3 Third distribution parameter.
 * @property seed Optional variable-specific seed. When omitted, a seed is derived
 * deterministically from the request-level seed.
 */
data class VariableSpec(
    val name: String? = null,
    val distribution: String,
    val param1: Double? = null,
    val param2: Double? = null,
    val param3: Double? = null,
    val seed: Long? = null
)

/**
 * Result of generating a sequence for one variable.
 *
 * @property name The resolved variable name.
 * @property distribution The normalized distribution name.
 * @property seed The seed used for this variable.
 * @property sequence The generated samples.
 */
data class VariableSequence(
    val name: String,
    val distribution: String,
    val seed: Long,
    val sequence: List<Double>
)

/**
 * Result of a single-variable generation request.
 *
 * @property sequence The generated samples.
 * @property seed The seed used to generate them.
 */
data class SequenceResult(val sequence: List<Double>, val seed: Long)

/**
 * Result of a multi-variable generation request.
 *
 * @property variables One result per requested variable, in request order.
 * @property seed The request-level seed used to derive per-variable seeds.
 */
data class MultiSequenceResult(
    val variables: List<VariableSequence>,
    val seed: Long
)
