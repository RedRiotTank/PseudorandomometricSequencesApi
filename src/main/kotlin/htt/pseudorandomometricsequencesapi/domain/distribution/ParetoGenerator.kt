package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.distribution.ParetoDistribution
import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Pareto_distribution">Pareto distribution</a>}.
 *
 * <p>This continuous probability distribution is known for the Pareto principle (or "80/20 rule").
 * It is used to model the distribution of wealth, sizes of human settlements, file sizes
 * distributed over the internet, and other phenomena exhibiting "heavy tails".</p>
 *
 * @param scale The **scale parameter** ($x_m$). It is the minimum possible value of $X$. Must be strictly positive ($x_m > 0$).
 * @param shape The **shape parameter** ($\alpha$), also known as the Pareto index. Must be strictly positive ($\alpha > 0$).
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator}.
 */
class ParetoGenerator(
    scale: Double,
    shape: Double,
    randomGenerator: RandomGenerator
) : SequenceGenerator {

    /** The internal Pareto distribution object from Apache Commons Math. */
    val distribution: ParetoDistribution

    init {
        require(scale > 0.0) { "Pareto 'scale' (param1) must be strictly positive (xm > 0)." }
        require(shape > 0.0) { "Pareto 'shape' (param2) must be strictly positive (alpha > 0)." }

        this.distribution = ParetoDistribution(randomGenerator, scale, shape)
    }

    override fun sample(): Double {
        return distribution.sample()
    }

    companion object {
        const val DISTRIBUTION_NAME = "pareto"

        /**
         * Factory method to create an instance of {@code ParetoGenerator}.
         *
         * <p>Default values:</p>
         * <ul>
         * <li>{@code param1} ($x_m$, scale): Defaults to **1.0**.</li>
         * <li>{@code param2} ($\alpha$, shape): Defaults to **1.0**.</li>
         * </ul>
         *
         * @param param1 The desired scale parameter ($x_m$) (can be {@code null}).
         * @param param2 The desired shape parameter ($\alpha$) (can be {@code null}).
         * @param generator The {@code RandomGenerator} instance.
         */
        fun create(param1: Double?, param2: Double?, generator: RandomGenerator): ParetoGenerator {
            val scale = param1 ?: 1.0
            val shape = param2 ?: 1.0

            return ParetoGenerator(scale, shape, generator)
        }
    }
}