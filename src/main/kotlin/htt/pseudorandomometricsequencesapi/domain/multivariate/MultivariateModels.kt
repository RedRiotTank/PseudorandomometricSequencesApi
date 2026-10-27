package htt.pseudorandomometricsequencesapi.domain.multivariate

/**
 * Description of a joint multivariate generation request.
 *
 * @property distribution The multivariate distribution name (case-insensitive).
 * @property mean Mean vector (multivariate normal and multivariate t).
 * @property covariance Covariance/scale matrix (multivariate normal and multivariate t).
 * @property degreesOfFreedom Degrees of freedom (multivariate t).
 * @property concentration Concentration parameters (Dirichlet).
 * @property trials Number of trials per sample (multinomial).
 * @property probabilities Category probabilities (multinomial).
 * @property variableNames Optional names for the components. Defaults to `x1`, `x2`, ...
 */
data class MultivariateSpec(
    val distribution: String,
    val mean: List<Double>? = null,
    val covariance: List<List<Double>>? = null,
    val degreesOfFreedom: Double? = null,
    val concentration: List<Double>? = null,
    val trials: Int? = null,
    val probabilities: List<Double>? = null,
    val variableNames: List<String>? = null
)

/**
 * Result of a joint multivariate generation request.
 *
 * @property distribution The normalized distribution name.
 * @property seed The seed used for generation.
 * @property variableNames The resolved component names.
 * @property sequence One vector per observation. Each inner list has one value per component.
 */
data class MultivariateSequenceResult(
    val distribution: String,
    val seed: Long,
    val variableNames: List<String>,
    val sequence: List<List<Double>>
)
