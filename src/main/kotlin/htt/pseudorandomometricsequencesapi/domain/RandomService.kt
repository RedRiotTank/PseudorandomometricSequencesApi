package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.*
import org.springframework.stereotype.Service
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import java.security.SecureRandom
import java.util.Random
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadLocalRandom

private typealias GeneratorFactory = (Double?, Double?, Random, RandomGenerator) -> SequenceGenerator

@Service
class RandomService {

    companion object {
        private val logger = LoggerFactory.getLogger(RandomService::class.java)

        private val SECURE_RANDOM_INSTANCE by lazy { SecureRandom() }

        private val GENERATOR_FACTORIES: Map<String, GeneratorFactory> = mapOf(
            UniformGenerator.DISTRIBUTION_NAME     to { p1, p2, jRand, _     -> UniformGenerator.create(p1, p2, jRand) },
            GaussianGenerator.DISTRIBUTION_NAME    to { p1, p2, jRand, _     -> GaussianGenerator.create(p1, p2, jRand) },
            ExponentialGenerator.DISTRIBUTION_NAME to { p1, _,  jRand, _     -> ExponentialGenerator.create(p1, jRand) },
            GammaGenerator.DISTRIBUTION_NAME       to { p1, p2, _,     cRand -> GammaGenerator.create(p1, p2, cRand) },
            LogNormalGenerator.DISTRIBUTION_NAME   to { p1, p2, _,     cRand -> LogNormalGenerator.create(p1, p2, cRand) },
            BetaGenerator.DISTRIBUTION_NAME        to { p1, p2, _,     cRand -> BetaGenerator.create(p1, p2, cRand) },
            WeibullGenerator.DISTRIBUTION_NAME     to { p1, p2, _,     cRand -> WeibullGenerator.create(p1, p2, cRand) },
            CauchyGenerator.DISTRIBUTION_NAME      to { p1, p2, _,     cRand -> CauchyGenerator.create(p1, p2, cRand) },
            TStudentGenerator.DISTRIBUTION_NAME    to { p1, _,  _,     cRand -> TStudentGenerator.create(p1, cRand) },
            BinomialGenerator.DISTRIBUTION_NAME    to { p1, p2, _,     cRand -> BinomialGenerator.create(p1, p2, cRand) }
        )
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

        val javaRandom: Random = when (typeName) {
            "secure" -> {
                logger.debug("Using generator type: SecureRandom")
                SECURE_RANDOM_INSTANCE
            }
            "general" ->{
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

        val factory = GENERATOR_FACTORIES[distributionName]
            ?: throw IllegalArgumentException("Invalid Distribution: $distributionName. Use one of the supported types.")

        val generator: SequenceGenerator = try {
            factory(param1, param2, javaRandom, commonsRandom)
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