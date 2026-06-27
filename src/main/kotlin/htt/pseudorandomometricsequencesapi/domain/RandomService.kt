package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import org.apache.commons.math3.random.JDKRandomGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

@Service
class RandomService(factories: List<DistributionFactory>) {

    companion object {
        private val logger = LoggerFactory.getLogger(RandomService::class.java)
        private val SECURE_RANDOM_INSTANCE by lazy { SecureRandom() }
    }

    private val registry: Map<String, DistributionFactory> = factories.associateBy { it.name }

    fun generateSequence(
        count: Int,
        type: String,
        distribution: String,
        param1: Double? = null,
        param2: Double? = null,
        param3: Double? = null
    ): List<Double> {
        require(count > 0) { "Count must be positive." }
        require(count <= 2000000) { "Count cannot be greater than 2,000,000" }

        logger.debug("Count validation successful: {}", count)

        val distributionName = distribution.lowercase()
        val typeName = type.lowercase()

        val javaRandom: Random = when (typeName) {
            "secure" -> {
                logger.debug("Using generator type: SecureRandom")
                SECURE_RANDOM_INSTANCE
            }
            "general" -> {
                logger.debug("Using generator type: Random (general)")
                ThreadLocalRandom.current()
            }
            else -> {
                logger.error("Invalid generator type: {}", typeName)
                throw IllegalArgumentException("Invalid type. Use 'secure' or 'general'.")
            }
        }

        val seedValue = javaRandom.nextLong()
        val commonsRandom = JDKRandomGenerator()
        commonsRandom.setSeed(seedValue)

        logger.debug("Apache Commons Math generator seed: {}", seedValue)

        val factory = registry[distributionName]
            ?: throw IllegalArgumentException("Invalid Distribution: $distributionName. Use one of the supported types.")

        val generator: SequenceGenerator = try {
            factory.create(param1, param2, param3, javaRandom, commonsRandom)
        } catch (e: IllegalArgumentException) {
            logger.error("Error creating generator for '{}' with Params: [{}, {}, {}]. Message: {}",
                distributionName, param1, param2, param3, e.message)
            throw e
        }

        logger.info("Built generator: {}. Samples to generate {}", generator::class.simpleName, count)

        return (1..count).map { generator.sample() }
    }
}
