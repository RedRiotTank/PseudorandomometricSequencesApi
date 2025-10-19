package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import htt.pseudorandomometricsequencesapi.domain.distribution.*
import org.springframework.stereotype.Service
import org.apache.commons.math3.random.JDKRandomGenerator
import java.security.SecureRandom
import java.util.Random

@Service
class RandomService {

    fun generateSequence(
        count: Int,
        type: String,
        distribution: String,
        param1: Double? = null,
        param2: Double? = null
    ): List<Double> {
        require(count > 0) { "Count must be positive." }

        val distributionName = distribution.lowercase()

        val javaRandom: Random = when (type.lowercase()) {
            "secure" -> SecureRandom()
            "general" -> Random()
            else -> throw IllegalArgumentException("Invalid type. Use 'secure' or 'general'.")
        }

        val commonsRandom = JDKRandomGenerator()
        commonsRandom.setSeed(javaRandom.nextLong())

        val generator: SequenceGenerator = when (distributionName) {

            UniformGenerator.DISTRIBUTION_NAME -> UniformGenerator.create(param1, param2, javaRandom)
            GaussianGenerator.DISTRIBUTION_NAME -> GaussianGenerator.create(param1, param2, javaRandom)
            ExponentialGenerator.DISTRIBUTION_NAME -> ExponentialGenerator.create(param1, javaRandom)

            GammaGenerator.DISTRIBUTION_NAME -> GammaGenerator.create(param1, param2, commonsRandom)
            LogNormalGenerator.DISTRIBUTION_NAME -> LogNormalGenerator.create(param1, param2, commonsRandom)
            BetaGenerator.DISTRIBUTION_NAME -> BetaGenerator.create(param1, param2, commonsRandom)

            else -> throw IllegalArgumentException(
                "Invalid Distribution: $distributionName. Use one of the supported types."
            )
        }

        return (1..count).map {
            generator.sample()
        }
    }
}