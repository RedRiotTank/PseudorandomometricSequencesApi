package htt.pseudorandomometricsequencesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Runtime limits for the public generation API.
 *
 * <p>Every value can be overridden with an environment variable using Spring Boot's
 * relaxed binding. For example, `api.limits.max-generation-millis` maps to
 * `API_LIMITS_MAX_GENERATION_MILLIS`. The defaults in this class are mirrored in
 * `application.properties`, which is the canonical list for operators.</p>
 */
@ConfigurationProperties(prefix = "api.limits")
class ApiLimitsProperties {

    /** Maximum number of samples (observations) requested by a single endpoint call. */
    var maxSamplesPerRequest: Int = 2_000_000

    /**
     * Maximum number of scalar values in one response. For POST /sequences this is
     * `count × variables`; for POST /multivariate it is `count × dimension`.
     */
    var maxValuesPerRequest: Int = 2_000_000

    /** Maximum number of independent variables accepted by POST /sequences. */
    var maxVariablesPerRequest: Int = 100

    /** Maximum number of components accepted by POST /multivariate. */
    var maxMultivariateDimension: Int = 100

    /**
     * Wall-clock budget for the sampling loop of one request.
     *
     * <p>A small calibration prefix is sampled first and the total cost is extrapolated.
     * Requests whose projected cost exceeds this budget are rejected with HTTP 422 before
     * the remaining samples are generated.</p>
     */
    var maxGenerationMillis: Long = 5_000

    /** Number of samples used to estimate the cost of a request before generating it. */
    var calibrationSamples: Int = 4_096

    /** Default number of empirical quantile samples per copula marginal. */
    var defaultQuantileSamples: Int = 100_000

    /** Minimum number of empirical quantile samples per copula marginal. */
    var minQuantileSamples: Int = 10_000

    /** Maximum number of empirical quantile samples per copula marginal. */
    var maxQuantileSamples: Int = 1_000_000

    /** Maximum total quantile samples (per marginal × number of marginals) in one copula request. */
    var maxTotalQuantileSamples: Long = 20_000_000

    /**
     * Total number of scalar values that may be under generation at the same time.
     * Protects both CPU and heap: two 2,000,000-value requests fit in the default
     * 4,000,000-value pool, while a flood of small requests is not needlessly blocked.
     */
    var maxConcurrentValues: Int = 4_000_000

    /** Global request budget for generation endpoints, per minute, across all clients. */
    var maxRequestsPerMinute: Int = 300

    /** Request budget for generation endpoints, per minute, per client IP. */
    var maxRequestsPerMinutePerIp: Int = 60

    /** Maximum accepted JSON request body size for POST generation endpoints. */
    var maxRequestBodyBytes: Long = 1_048_576

    /** Maximum length of a variable/component name, to keep logs and payloads bounded. */
    var maxVariableNameLength: Int = 128

    init {
        require(maxSamplesPerRequest > 0) { "api.limits.max-samples-per-request must be positive." }
        require(maxValuesPerRequest > 0) { "api.limits.max-values-per-request must be positive." }
        require(maxVariablesPerRequest > 0) { "api.limits.max-variables-per-request must be positive." }
        require(maxMultivariateDimension > 0) {
            "api.limits.max-multivariate-dimension must be positive."
        }
        require(maxGenerationMillis > 0) { "api.limits.max-generation-millis must be positive." }
        require(calibrationSamples > 0) { "api.limits.calibration-samples must be positive." }
        require(defaultQuantileSamples > 0) {
            "api.limits.default-quantile-samples must be positive."
        }
        require(minQuantileSamples > 0) { "api.limits.min-quantile-samples must be positive." }
        require(maxQuantileSamples >= minQuantileSamples) {
            "api.limits.max-quantile-samples must be at least min-quantile-samples."
        }
        require(defaultQuantileSamples in minQuantileSamples..maxQuantileSamples) {
            "api.limits.default-quantile-samples must be within min/max quantile samples."
        }
        require(maxTotalQuantileSamples > 0) {
            "api.limits.max-total-quantile-samples must be positive."
        }
        require(maxConcurrentValues > 0) {
            "api.limits.max-concurrent-values must be positive."
        }
        require(maxRequestsPerMinute > 0) { "api.limits.max-requests-per-minute must be positive." }
        require(maxRequestsPerMinutePerIp > 0) {
            "api.limits.max-requests-per-minute-per-ip must be positive."
        }
        require(maxRequestBodyBytes > 0) { "api.limits.max-request-body-bytes must be positive." }
        require(maxVariableNameLength > 0) {
            "api.limits.max-variable-name-length must be positive."
        }
    }

    val maxGenerationNanos: Long
        get() = maxGenerationMillis * 1_000_000L
}
