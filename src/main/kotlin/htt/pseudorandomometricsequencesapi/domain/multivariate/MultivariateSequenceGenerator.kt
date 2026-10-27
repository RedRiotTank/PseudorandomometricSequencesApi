package htt.pseudorandomometricsequencesapi.domain.multivariate

/**
 * A generator that produces one vector-valued observation per call.
 *
 * <p>Unlike [htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator],
 * the values inside a sample are jointly distributed and may be correlated.</p>
 */
interface MultivariateSequenceGenerator {
    /** Number of components in each generated vector. */
    val dimension: Int

    /** Generates the next joint observation. */
    fun sample(): DoubleArray
}
