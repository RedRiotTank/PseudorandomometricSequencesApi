package htt.pseudorandomometricsequencesapi.domain.distribution

import java.util.Random

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Continuous_uniform_distribution">Continuous Uniform distribution</a>}.
 *
 * <p>This class implements the {@code SequenceGenerator} interface and generates samples
 * uniformly across a specified closed interval $[a, b]$. In a uniform distribution, every
 * value within the interval is equally likely to be sampled.</p>
 *
 * <p>The distribution is characterized by the minimum ($a$) and maximum ($b$) bounds of the interval.
 * The probability density function (PDF) is constant over this interval:
 * $$ f(x; a, b) = \begin{cases} \frac{1}{b - a} & \text{for } a \le x \le b \\ 0 & \text{otherwise} \end{cases} $$</p>
 *
 * @param min The lower bound ($a$) of the distribution's interval. This value is inclusive.
 * @param max The upper bound ($b$) of the distribution's interval. This value is exclusive for the
 * generated sample, as {@code java.util.Random.nextDouble()} returns values in $[0.0, 1.0)$.
 * @param javaRandom The underlying {@code java.util.Random} instance used to generate
 * the base uniform pseudorandom numbers in $[0, 1)$.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Continuous_uniform_distribution">Continuous Uniform Distribution</a>
 * @see java.util.Random#nextDouble()
 */
class UniformGenerator(
    private val min: Double,
    private val max: Double,
    private val javaRandom: Random
) : SequenceGenerator {

    /**
     * Initializes the generator and validates the interval bounds.
     *
     * @throws IllegalArgumentException if {@code min} is greater than or equal to {@code max} ($a \ge b$).
     */
    init {
        require(min < max) { "Uniform 'min' (param1) must be less than 'max' (param2)." }
    }

    /**
     * Generates and returns the next pseudorandom sample from the Uniform distribution
     * over the interval $[\text{min}, \text{max})$.
     *
     * <p>The sample is computed using the formula:
     * $$ X = a + (b - a) U $$
     * where $U \sim \text{Uniform}(0, 1)$, $a = \text{min}$, and $b = \text{max}$.</p>
     *
     * @return A pseudorandom {@code Double} value sampled from $\text{Uniform}(\text{min}, \text{max})$.
     */
    override fun sample(): Double {
        return min + (max - min) * javaRandom.nextDouble()
    }

    /**
     * Contains constants and factory methods for {@code UniformGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "uniform".
         */
        const val DISTRIBUTION_NAME = "uniform"

        /**
         * Factory method to create an instance of {@code UniformGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} (min, $a$): Defaults to <b>0.0</b>.</li>
         * <li>{@code param2} (max, $b$): Defaults to <b>1.0</b>.</li>
         * </ul>
         * <p>Using the defaults results in the <b>Standard Uniform Distribution</b>,
         * $\text{Uniform}(0, 1)$.</p>
         *
         * @param param1 The desired lower bound ($\text{min}$) (can be {@code null}).
         * @param param2 The desired upper bound ($\text{max}$) (can be {@code null}).
         * @param javaRandom The {@code java.util.Random} instance to be used by the generator.
         * @return A new {@code UniformGenerator} instance.
         * @throws IllegalArgumentException if the effective $\text{min}$ is not less than the effective $\text{max}$.
         */
        fun create(param1: Double?, param2: Double?, javaRandom: Random): UniformGenerator {
            val min = param1 ?: 0.0
            val max = param2 ?: 1.0

            return UniformGenerator(min, max, javaRandom)
        }
    }
}