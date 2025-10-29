package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import htt.pseudorandomometricsequencesapi.domain.distribution.*
import org.springframework.stereotype.Service
import org.apache.commons.math3.random.JDKRandomGenerator
import java.security.SecureRandom
import java.util.Random
import org.slf4j.LoggerFactory

@Service
class RandomService {

    companion object {
        private val logger = LoggerFactory.getLogger(RandomService::class.java)
    }

    fun generateSequence(
        count: Int,
        type: String,
        distribution: String,
        param1: Double? = null,
        param2: Double? = null
    ): List<Double> {
        require(count > 0) { "Count must be positive." }
        require(count <= 2000000) {"Count cannot be greater than 2,000,000"}

        logger.debug("Count validation successful: {}", count)

        val distributionName = distribution.lowercase()
        val typeName = type.lowercase()

        val javaRandom: Random = when (type.lowercase()) {
            "secure" -> {
                logger.debug("Using generator type: SecureRandom")
                SecureRandom()
            }
            "general" ->{
                logger.debug("Using generator type: Random (general)")
                Random()
            }
            else -> {
                logger.error("Invalid generator type: {}", typeName)
                throw IllegalArgumentException("Invalid type. Use 'secure' or 'general'.")
            }
        }

        val seedValue = javaRandom.nextLong()
        val commonsRandom = JDKRandomGenerator()

        commonsRandom.setSeed(seedValue)

        commonsRandom.setSeed(javaRandom.nextLong())
        logger.debug("Apache Commons Math generator seed: {}", seedValue)

        val generator: SequenceGenerator = try {
            when (distributionName) {
                UniformGenerator.DISTRIBUTION_NAME -> UniformGenerator.create(param1, param2, javaRandom)
                GaussianGenerator.DISTRIBUTION_NAME -> GaussianGenerator.create(param1, param2, javaRandom)
                ExponentialGenerator.DISTRIBUTION_NAME -> ExponentialGenerator.create(param1, javaRandom)
                GammaGenerator.DISTRIBUTION_NAME -> GammaGenerator.create(param1, param2, commonsRandom)
                LogNormalGenerator.DISTRIBUTION_NAME -> LogNormalGenerator.create(param1, param2, commonsRandom)
                BetaGenerator.DISTRIBUTION_NAME -> BetaGenerator.create(param1, param2, commonsRandom)
                WeibullGenerator.DISTRIBUTION_NAME -> WeibullGenerator.create(param1, param2, commonsRandom)
                CauchyGenerator.DISTRIBUTION_NAME -> CauchyGenerator.create(param1, param2, commonsRandom)
                TStudentGenerator.DISTRIBUTION_NAME -> TStudentGenerator.create(param1, commonsRandom)
                BinomialGenerator.DISTRIBUTION_NAME -> BinomialGenerator.create(param1, param2, commonsRandom)
                else -> throw IllegalArgumentException(
                    "Invalid Distribution: $distributionName. Use one of the supported types."
                )
            }
        } catch (e: IllegalArgumentException) {

            logger.error("Error creating generator for '{}' with Params: [{}, {}]. Message: {}",
                distributionName, param1, param2, e.message)
            throw e
        }

        logger.info("Built generator: {}. Samples to generate {}",
            generator::class.simpleName, count)

        return (1..count).map {
            generator.sample()
        }
    }
}