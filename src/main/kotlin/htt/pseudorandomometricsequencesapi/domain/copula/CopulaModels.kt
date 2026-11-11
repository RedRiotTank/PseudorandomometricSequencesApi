package htt.pseudorandomometricsequencesapi.domain.copula

/**
 * Definition of one marginal variable in a Gaussian-copula request.
 *
 * @property name Optional unique name. Defaults to `variable1`, `variable2`, ...
 * @property distribution Any registered univariate distribution name.
 * @property param1 First distribution parameter.
 * @property param2 Second distribution parameter.
 * @property param3 Third distribution parameter.
 */
data class CopulaVariableSpec(
    val name: String? = null,
    val distribution: String,
    val param1: Double? = null,
    val param2: Double? = null,
    val param3: Double? = null
)

/**
 * Generated sequence for one marginal in a Gaussian-copula request.
 */
data class CopulaVariableSequence(
    val name: String,
    val distribution: String,
    val sequence: List<Double>
)

/**
 * Result of a Gaussian-copula generation request.
 *
 * @property seed Request-level seed.
 * @property correlation Normalized correlation matrix used by the copula.
 * @property quantileSamples Number of marginal samples used to build each empirical quantile table.
 * @property variables One dependent sequence per marginal.
 */
data class CopulaResult(
    val seed: Long,
    val correlation: List<List<Double>>,
    val quantileSamples: Int,
    val variables: List<CopulaVariableSequence>
)
