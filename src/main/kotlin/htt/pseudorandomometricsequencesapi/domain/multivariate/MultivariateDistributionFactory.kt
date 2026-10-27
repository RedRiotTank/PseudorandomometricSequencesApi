package htt.pseudorandomometricsequencesapi.domain.multivariate

import org.apache.commons.math3.random.RandomGenerator
import java.util.Random

/**
 * Contract for a self-registering joint multivariate distribution factory.
 *
 * <p>Implementations are Spring components and are discovered automatically by
 * [htt.pseudorandomometricsequencesapi.domain.MultivariateRandomService].</p>
 */
interface MultivariateDistributionFactory {
    val name: String

    /** One-line description of the distribution's parameters for documentation. */
    val description: String get() = "No parameters documented yet."

    fun create(
        spec: MultivariateSpec,
        javaRandom: Random,
        commonsRandom: RandomGenerator
    ): MultivariateSequenceGenerator
}
