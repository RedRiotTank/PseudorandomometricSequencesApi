package htt.pseudorandomometricsequencesapi.domain.distribution

import java.util.Random


/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Normal_distribution">Gaussian (Normal) distribution</a>}.
 *
 * <p>This class implements the {@code SequenceGenerator} interface and uses the
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Random.html#nextGaussian()">{@code java.util.Random.nextGaussian()}</a>
 * method, which is based on the <a href="https://en.wikipedia.org/wiki/Box%E2%80%93Muller_transform">Box-Muller transform</a>,
 * to produce samples.</p>
 *
 * <p>A Gaussian distribution is fully characterized by two parameters: the
 * <b>mean</b> ($\mu$) and the <b>standard deviation</b> ($\sigma$).
 * The probability density function (PDF) is given by:
 * $$ f(x) = \frac{1}{\sigma \sqrt{2\pi}} e^{-\frac{1}{2} \left(\frac{x - \mu}{\sigma}\right)^2} $$</p>
 *
 * @param mean The <b>arithmetic mean</b> ($\mu$) of the distribution, which is also
 * its expected value. This parameter dictates the center of the distribution.
 * @param stddev The <b>standard deviation</b> ($\sigma$) of the distribution. It measures
 * the amount of variation or dispersion of a set of values.
 * Must be a positive value ($&gt; 0$).
 * @param javaRandom The underlying {@code java.util.Random} instance used to generate
 * the base pseudorandom numbers.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Normal_distribution">Normal Distribution (Gaussian)</a>
 * @see java.util.Random#nextGaussian()
 */
class GaussianGenerator(
    private val mean: Double,
    private val stddev: Double,
    private val javaRandom: Random
) : SequenceGenerator {

    /**
     * Initializes the generator and validates the standard deviation.
     *
     * @throws IllegalArgumentException if {@code stddev} is not positive ($\sigma \le 0$).
     */
    init {
        require(stddev > 0.0) { "Gaussian 'stddev' (param2) must be positive." }
    }

    /**
     * Generates and returns the next pseudorandom sample from the Gaussian distribution.
     *
     * <p>The sample is computed by scaling and shifting the result of
     * {@code javaRandom.nextGaussian()} (which returns a sample from the standard
     * normal distribution $\mathcal{N}(0, 1)$) according to the formula:
     * $$ X = \mu + \sigma Z $$
     * where $Z \sim \mathcal{N}(0, 1)$, $\mu$ is the mean, and $\sigma$ is the
     * standard deviation.</p>
     *
     * @return A pseudorandom {@code Double} value sampled from $\mathcal{N}(\text{mean}, \text{stddev}^2)$.
     */
    override fun sample(): Double {
        return mean + stddev * javaRandom.nextGaussian()
    }

    /**
     * Contains constants and factory methods for {@code GaussianGenerator}.
     */
    companion object {

        /**
         * The canonical name for the distribution this class generates, "gaussian".
         */
        const val DISTRIBUTION_NAME = "gaussian"

        /**
         * Factory method to create an instance of {@code GaussianGenerator}.
         *
         * <p>It applies default values if the parameters are {@code null}:</p>
         * <ul>
         * <li>{@code param1} (mean, $\mu$): Defaults to <b>0.0</b>.</li>
         * <li>{@code param2} (stddev, $\sigma$): Defaults to <b>1.0</b>.</li>
         * </ul>
         * <p>Using the defaults results in a <b>Standard Normal Distribution</b>, $\mathcal{N}(0, 1)$.</p>
         *
         * @param param1 The desired mean ($\mu$) for the distribution (can be {@code null}).
         * @param param2 The desired standard deviation ($\sigma$) for the distribution (can be {@code null}).
         * @param javaRandom The {@code java.util.Random} instance to be used by the generator.
         * @return A new {@code GaussianGenerator} instance.
         * @throws IllegalArgumentException if the effective {@code stddev} is not positive.
         */
        fun create(param1: Double?, param2: Double?, javaRandom: Random): GaussianGenerator {
            val mean = param1 ?: 0.0
            val stddev = param2 ?: 1.0

            return GaussianGenerator(mean, stddev, javaRandom)
        }
    }
}