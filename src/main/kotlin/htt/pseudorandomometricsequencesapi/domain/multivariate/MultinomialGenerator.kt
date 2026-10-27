package htt.pseudorandomometricsequencesapi.domain.multivariate

import org.apache.commons.math3.distribution.BinomialDistribution
import org.apache.commons.math3.random.RandomGenerator
import org.springframework.stereotype.Component
import kotlin.math.abs

/**
 * Joint multinomial distribution.
 *
 * <p>Each sample contains one count per category. The counts are non-negative integers
 * and always sum to the configured number of trials. Sampling uses the conditional
 * binomial method, which avoids drawing every individual trial.</p>
 */
class MultinomialGenerator(
    private val trials: Int,
    probabilities: DoubleArray,
    randomGenerator: RandomGenerator
) : MultivariateSequenceGenerator {

    override val dimension: Int = probabilities.size
    private val probabilities: DoubleArray
    private val randomGenerator: RandomGenerator = randomGenerator

    init {
        require(trials >= 0) { "Multinomial 'trials' must be non-negative." }
        require(probabilities.isNotEmpty()) {
            "Multinomial 'probabilities' must not be empty."
        }
        require(probabilities.all { it >= 0.0 && it.isFinite() }) {
            "Multinomial 'probabilities' must be non-negative and finite."
        }

        val sum = probabilities.sum()
        require(abs(sum - 1.0) <= 1e-6) {
            "Multinomial 'probabilities' must sum to 1.0 (sum was $sum)."
        }
        this.probabilities = DoubleArray(probabilities.size) { probabilities[it] / sum }
    }

    override fun sample(): DoubleArray {
        val result = DoubleArray(dimension)
        var remainingTrials = trials
        var remainingProbability = 1.0

        for (index in 0 until dimension - 1) {
            if (remainingTrials == 0) {
                break
            }

            val probability = if (remainingProbability > 0.0) {
                (probabilities[index] / remainingProbability).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            val count = when {
                probability <= 0.0 -> 0
                probability >= 1.0 -> remainingTrials
                else -> BinomialDistribution(randomGenerator, remainingTrials, probability).sample()
            }

            result[index] = count.toDouble()
            remainingTrials -= count
            remainingProbability -= probabilities[index]
        }

        result[dimension - 1] = remainingTrials.toDouble()
        return result
    }
}

@Component
class MultinomialFactory : MultivariateDistributionFactory {

    override val name = "multinomial"

    override val description =
        "trials = number of trials n (>= 0), probabilities = category probability vector p (must sum to 1.0)"

    override fun create(
        spec: MultivariateSpec,
        javaRandom: java.util.Random,
        commonsRandom: RandomGenerator
    ): MultivariateSequenceGenerator {
        val trials = spec.trials
            ?: throw IllegalArgumentException("Multinomial requires 'trials'.")
        val probabilities = spec.probabilities
            ?: throw IllegalArgumentException("Multinomial requires 'probabilities'.")

        return MultinomialGenerator(
            trials = trials,
            probabilities = probabilities.toDoubleArray(),
            randomGenerator = commonsRandom
        )
    }
}
