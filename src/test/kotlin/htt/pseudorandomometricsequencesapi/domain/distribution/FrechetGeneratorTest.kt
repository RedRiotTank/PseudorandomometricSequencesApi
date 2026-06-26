package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.special.Gamma
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.ln

/**
 * For Frechet with alpha=1, sigma=1, mu=0:
 * X = mu + sigma * (-ln U)^(-1/alpha) = 0 + 1 * (-ln 0.5)^(-1) = 1/ln2 ≈ 1.4427
 */
class FakeCommonsRandomForFrechet : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 1
    override fun nextInt(n: Int) = 1
    override fun nextLong() = 1L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = true
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
}

class FrechetGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForFrechet()

    @Test
    fun `create should use default values (alpha=1, sigma=1, mu=0) when parameters are null`() {
        val generator = FrechetGenerator.create(null, null, null, fakeRandom)
        assertNotNull(generator)
        // alpha=1, sigma=1, mu=0, U=0.5: X = 0 + 1 * (-ln(0.5))^(-1) = 1/ln2
        val expected = 1.0 / ln(2.0)
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "FrechetGenerator should fail with alpha: {0} or sigma: {1}")
    @CsvSource(
        "0.0, 1.0, 0.0",   // alpha <= 0
        "-1.0, 1.0, 0.0",  // alpha negative
        "1.0, 0.0, 0.0",   // sigma <= 0
        "1.0, -1.0, 0.0"   // sigma negative
    )
    fun `should throw exception for invalid parameters`(alpha: Double, sigma: Double, mu: Double) {
        assertThrows<IllegalArgumentException> {
            FrechetGenerator.create(alpha, sigma, mu, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = mu + sigma times (-ln U) to the power (-1 over alpha) correctly`() {
        val alpha = 1.0
        val sigma = 1.0
        val mu = 0.0
        val generator = FrechetGenerator.create(alpha, sigma, mu, fakeRandom)
        // U=0.5: X = 0 + 1 * (-ln(0.5))^(-1/1) = 1 / ln2 ≈ 1.4427
        val expected = 1.0 / ln(2.0)
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to theoretical mean for alpha=2 sigma=1 mu=0`() {
        // Mean for Frechet(alpha=2, sigma=1, mu=0) = mu + sigma * Gamma(1 - 1/alpha) = Gamma(0.5) = sqrt(pi) ≈ 1.7725
        val theoreticalMean = Gamma.gamma(0.5)  // sqrt(pi) ≈ 1.7725
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = FrechetGenerator.create(2.0, 1.0, 0.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}
          