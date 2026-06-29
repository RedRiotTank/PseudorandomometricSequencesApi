package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.SequenceGenerator
import org.apache.commons.math3.random.JDKRandomGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

data class SequenceResult(val sequence: List<Double>, val seed: Long)

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
        param3: Double? = null,
        seed: Long? = null
    ): SequenceResult {
        require(count > 0) { "Count must be positive." }
        require(count <= 2000000) { "Count cannot be greater than 2,000,000" }

        logger.debug("Count validation successful: {}", count)

        val distributionName = distribution.lowercase()
        val typeName = type.lowercase()

        val seedValue = seed ?: when (typeName) {
            "secure" -> {
                SECURE_RANDOM_INSTANCE.nextLong()
            }
            "general" -> {
                ThreadLocalRandom.current().nextLong()
            }
            else -> {
                logger.error("Invalid generator type: {}", typeName)
                throw IllegalArgumentException("Invalid type. Use 'secure' or 'general'.")
            }
        }

        val javaRandom: Random = when (typeName) {
            "secure" -> {
                logger.debug("Using generator type: SecureRandom with seed: {}", seedValue)
                try {
                    SecureRandom.getInstance("SHA1PRNG").apply {
                        setSeed(seedValue)
                    }
                } catch (e: Exception) {
                    logger.warn("SHA1PRNG SecureRandom not available, falling back to java.util.Random: {}", e.message)
                    java.util.Random(seedValue)
                }
            }
            "general" -> {
                logger.debug("Using generator type: Random (general) with seed: {}", seedValue)
                java.util.Random(seedValue)
            }
            else -> {
                logger.error("Invalid generator type: {}", typeName)
                throw IllegalArgumentException("Invalid type. Use 'secure' or 'general'.")
            }
        }

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

        val sequence = (1..count).map { generator.sample() }
        return SequenceResult(sequence, seedValue)
    }
}
