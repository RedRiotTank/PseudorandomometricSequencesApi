package htt.pseudorandomometricsequencesapi



import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Random

@Service
class RandomService {


    fun generateSequence(count: Int, type: String, distribution: String): List<Double> {
        require(count > 0) { "Count must be positive." }


        val randomGenerator: Random = when (type.lowercase()) {
            "secure" -> SecureRandom() // crip secure
            "general" -> Random()      // gen
            else -> throw IllegalArgumentException("Invalid type. Use 'secure' or 'general'.")
        }

        return (1..count).map {
            when (distribution.lowercase()) {
                "uniform" -> randomGenerator.nextDouble()
                "gaussian" -> randomGenerator.nextGaussian()
                else -> throw IllegalArgumentException("Invalid Distribution. Use 'uniform' or 'gaussian'.")
            }
        }
    }
}