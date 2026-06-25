package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import java.util.Random

/**
 * Contract for a self-registering probability distribution factory.
 *
 * Any Spring {@code @Component} implementing this interface is automatically discovered
 * and registered in both {@code RandomService} (sampling) and the Swagger UI (documentation)
 * via constructor injection — no manual wiring needed.
 */
interface DistributionFactory {
    val name: String

    /** One-line description of the distribution's parameters for Swagger. E.g. "param1 = mean μ (def. 0.0), param2 = std. deviation σ (def. 1.0)". */
    val description: String get() = "No parameters documented yet."

    fun create(param1: Double?, param2: Double?, param3: Double?, javaRandom: Random, commonsRandom: RandomGenerator): SequenceGenerator
}
