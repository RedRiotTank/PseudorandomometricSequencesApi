package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * For Lomax with alpha=1, lambda=1:
 * X = lambda * ((1-U)^(-1/alpha) - 1) = 1 * (0.5^(-1) - 1) = 2 - 1 = 1.0
 */
class FakeCommonsRandomForLomax : RandomGenerator {
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

class LomaxGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForLomax()

    @Test
    fun `create should use default values (alpha=1 0, lambda=1 0) when parameters are null`() {
        val generator = LomaxGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // alpha=1, lambda=1, U=0.5: X = 1 * (0.5^(-1) - 1) = 1.0
        assertEquals(1.0, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "LomaxGenerator should fail with alpha: {0} or lambda: {1}")
    @CsvSource(
        "0.0, 1.0",    // alpha <= 0
        "-1.0, 1.0",   // alpha negative
        "1.0, 0.0",    // lambda <= 0
        "1.0, -1.0"    // lambda negative
    )
    fun `should throw exception for invalid parameters`(alpha: Double, lambda: Double) {
        assertThrows<IllegalArgumentException> {
            LomaxGenerator.create(alpha, lambda, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = lambda times ((1-U) to the power (-1 over alpha) minus 1) correctly`() {
        val generator = LomaxGenerator.create(1.0, 1.0, fakeRandom)
        // U=0.5: X = 1 * ((1-0.5)^(-1/1) - 1) = (0.5^-1) - 1 = 2 - 1 = 1.0
        assertEquals(1.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to theoretical mean for alpha=3 lambda=2`() {
        // Mean for Lomax(alpha=3, lambda=2) = lambda / (alpha - 1) = 2 / 2 = 1.0
        val theoreticalMean = 1.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = LomaxGenerator.create(3.0, 2.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}