package htt.pseudorandomometricsequencesapi.domain

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

/**
 * Shared creation and validation of the underlying random number generators.
 */
object RandomSourceFactory {

    private val logger = LoggerFactory.getLogger(RandomSourceFactory::class.java)
    private val SECURE_RANDOM_INSTANCE by lazy { SecureRandom() }

    const val INVALID_TYPE_MESSAGE = "Invalid type. Use 'secure' or 'general'."

    fun normalizeType(type: String): String {
        val typeName = type.lowercase()
        if (typeName != "general" && typeName != "secure") {
            logger.error("Invalid generator type: {}", typeName)
            throw IllegalArgumentException(INVALID_TYPE_MESSAGE)
        }
        return typeName
    }

    fun nextSeed(typeName: String): Long = when (typeName) {
        "secure" -> SECURE_RANDOM_INSTANCE.nextLong()
        "general" -> ThreadLocalRandom.current().nextLong()
        else -> throw IllegalArgumentException(INVALID_TYPE_MESSAGE)
    }

    fun create(typeName: String, seed: Long): RandomSources {
        val javaRandom: Random = when (typeName) {
            "secure" -> {
                logger.debug("Using generator type: SecureRandom with seed: {}", seed)
                try {
                    SecureRandom.getInstance("SHA1PRNG").apply { setSeed(seed) }
                } catch (e: Exception) {
                    logger.warn(
                        "SHA1PRNG SecureRandom not available, falling back to java.util.Random: {}",
                        e.message
                    )
                    Random(seed)
                }
            }

            "general" -> {
                logger.debug("Using generator type: Random (general) with seed: {}", seed)
                Random(seed)
            }

            else -> throw IllegalArgumentException(INVALID_TYPE_MESSAGE)
        }

        val commonsRandom = JDKRandomGenerator().apply { setSeed(seed) }
        logger.debug("Apache Commons Math generator seed: {}", seed)

        return RandomSources(javaRandom, commonsRandom)
    }
}

data class RandomSources(
    val javaRandom: Random,
    val commonsRandom: RandomGenerator
)
