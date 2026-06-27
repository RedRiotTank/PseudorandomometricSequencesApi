package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.ln
import kotlin.math.pow

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Tukey_lambda_distribution">Tukey Lambda distribution</a>}.
 *
 * <p>Uniquely defined <em>only</em> by its quantile function — it has no closed-form PDF or CDF.
 * A single shape parameter $\lambda$ controls the entire character of the distribution:</p>
 * <ul>
 *   <li>$\lambda \approx -1$: approximates Cauchy (very heavy tails)</li>
 *   <li>$\lambda = 0$: Logistic</li>
 *   <li>$\lambda \approx 0.14$: approximates Normal</li>
 *   <li>$\lambda = 0.5$: U-shaped (bimodal)</li>
 *   <li>$\lambda = 1$: Uniform on $[-1, 1]$</li>
 * </ul>
 *
 * <p><strong>Algorithm</strong> — direct quantile (inverse-CDF) method. Since the quantile
 * function is the definition of the distribution, sampling is trivial and exact:
 * <pre>
 *   U ~ Uniform(0, 1)
 *   if λ ≠ 0:  return (U^λ − (1−U)^λ) / λ
 *   if λ = 0:  return ln(U / (1−U))           // limit as λ → 0
 * </pre>
 * This is a rare case where the distribution is <em>easier</em> to sample than to describe
 * analytically, demonstrating the power of the inverse-transform method.</p>
 *
 * @param lambda The shape parameter ($\lambda$). Default 0.0 (Logistic).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Tukey_lambda_distribution">Tukey Lambda Distribution</a>
 */
class TukeyLambdaGenerator(
    private val lambda: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    /**
     * Samples directly from the quantile function $Q(U;\lambda)$.
     */
    override fun sample(): Double {
        val u = randomGenerator.nextDouble()
        return if (Math.abs(lambda) < 1e-10) {
            ln(u / (1.0 - u))
        } else {
            (u.pow(lambda) - (1.0 - u).pow(lambda)) / lambda
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "tukey-lambda"

        /**
         * Default: $\lambda = 0.0$ (Logistic shape). Try $\lambda = 0.14$ for Normal-like.
         */
        fun create(param1: Double?, generator: RandomGenerator): TukeyLambdaGenerator {
            val lambda = param1 ?: 0.0
            return TukeyLambdaGenerator(lambda, generator)
        }
    }
}

@org.springframework.stereotype.Component
class TukeyLambdaFactory : DistributionFactory {
    override val name = TukeyLambdaGenerator.DISTRIBUTION_NAME
    override val description = "param1 = shape λ (def. 0.0; 0=logistic, 0.14≈normal, 0.5=U-shaped, 1=uniform on [-1,1])"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        TukeyLambdaGenerator.create(p1, commonsRandom)
}
