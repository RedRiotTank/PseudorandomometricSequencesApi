package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator

/**
 * Represents a generator for pseudorandom numbers following a
 * {@link <a href="https://en.wikipedia.org/wiki/Wigner_semicircle_distribution">Wigner Semicircle distribution</a>}.
 *
 * <p>Defined on $[-R, R]$ with PDF $f(x) = \frac{2}{\pi R^2}\sqrt{R^2 - x^2}$, whose graph is a
 * semicircle of radius $R$. Arises in random matrix theory as the limiting spectral distribution
 * of large symmetric random matrices (Wigner's semicircle law, 1955).</p>
 *
 * <p><strong>Algorithm</strong> — geometric rejection sampling on the unit disk:
 * <pre>
 *   repeat:
 *     X ~ Uniform(−R, R)
 *     Y ~ Uniform(−R, R)
 *   until X² + Y² ≤ R²          // accept points inside the disk
 *   return X                    // X-coordinate has the semicircle marginal
 * </pre>
 * <em>Why it works:</em> a uniform distribution on the disk of radius $R$ has marginal density
 * $f(x) = \frac{2\sqrt{R^2-x^2}}{\pi R^2}$, exactly the Wigner semicircle PDF. Acceptance
 * rate $= \pi/4 \approx 78.5\%$.</p>
 *
 * @param radius The radius parameter ($R$). Must be strictly positive.
 * @param randomGenerator The underlying {@link org.apache.commons.math3.random.RandomGenerator} instance.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Wigner_semicircle_distribution">Wigner Semicircle Distribution</a>
 */
class WignerSemicircleGenerator(
    private val radius: Double,
    private val randomGenerator: RandomGenerator
) : SequenceGenerator {

    private val r2: Double = radius * radius

    /**
     * Rejection samples a point uniformly inside the disk; returns its x-coordinate.
     */
    override fun sample(): Double {
        while (true) {
            val x = randomGenerator.nextDouble() * 2.0 * radius - radius
            val y = randomGenerator.nextDouble() * 2.0 * radius - radius
            if (x * x + y * y <= r2) return x
        }
    }

    companion object {
        const val DISTRIBUTION_NAME = "wigner-semicircle"

        /**
         * Default: $R = 1.0$.
         */
        fun create(param1: Double?, generator: RandomGenerator): WignerSemicircleGenerator {
            val r = param1 ?: 1.0
            require(r > 0.0) { "Wigner Semicircle 'radius' (param1) must be strictly positive." }
            return WignerSemicircleGenerator(r, generator)
        }
    }
}

@org.springframework.stereotype.Component
class WignerSemicircleFactory : DistributionFactory {
    override val name = WignerSemicircleGenerator.DISTRIBUTION_NAME
    override val description = "param1 = radius R (def. 1.0) — output in [-R, R]"
    override fun create(p1: Double?, p2: Double?, p3: Double?, javaRandom: java.util.Random, commonsRandom: org.apache.commons.math3.random.RandomGenerator) =
        WignerSemicircleGenerator.create(p1, commonsRandom)
}
