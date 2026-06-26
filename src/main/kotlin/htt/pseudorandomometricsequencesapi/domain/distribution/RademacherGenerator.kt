package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following the
 * {@link <a href="https://en.wikipedia.org/wiki/Rademacher_distribution">Rademacher distribution</a>}.
 *
 * <p>Returns $-1$ or $+1$ each with probability $1/2$. Used extensively in machine learning
 * (random projections, Johnson-Lindenstrauss lemma), compressed sensing, and as the building
 * block for Rademacher complexity in statistical learning theory.</p>
 *
 * <p><strong>Algorithm</strong>:
 * <pre>
 *   return −1  if U < 0.5,  else  +1
 * </pre>
 * Mean $= 0$, Variance $= 1$.</p>
 *
 * @param randomGenerator The underlying RNG.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Rademacher_distribution">Rademacher Distribution</a>
 */
class RademacherGenerator(
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    override fun sample(): Double = if (randomGenerator.nextDouble() < 0.5) -1.0 else 1.0

    companion object {
        const val DISTRIBUTION_NAME = "rademacher"

        fun create(generator: RandomGenerator): RademacherGenerator = RademacherGenerator(generator)
    }
}

@org.springframework.stereotype.Component
class RademacherFactory : DistributionFactory {
    override val name = RademacherGenerator.DISTRIBUTION_NAME
    override val description = "no parameters — returns -1 or +1 with equal probability"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        RademacherGenerator.create(commonsRandom)
}
