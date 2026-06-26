package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * For MaxwellBoltzmann with sigma=1, nextGaussian always returns 1.0:
 * X = 1 * sqrt(1^2 + 1^2 + 1^2) = sqrt(3) ≈ 1.7321
 */
class FakeCommonsRandomForMaxwellBoltzmann : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 1.0
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

class MaxwellBoltzmannGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForMaxwellBoltzmann()

    @Test
    fun `create should use default value (sigma=1 0) when parameter is null`() {
        val generator = MaxwellBoltzmannGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        // nextGaussian=1.0 for all three: X = 1 * sqrt(3) ≈ 1.7321
        assertEquals(sqrt(3.0), generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "MaxwellBoltzmannGenerator should fail with sigma: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if sigma is not positive`(sigma: Double) {
        assertThrows<IllegalArgumentException> {
            MaxwellBoltzmannGenerator.create(sigma, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = sigma times sqrt(X1 squared + X2 squared + X3 squared) correctly`() {
        val sigma = 2.0
        val generator = MaxwellBoltzmannGenerator.create(sigma, fakeRandom)
        // nextGaussian=1.0 for all three: X = 2 * sqrt(1+1+1) = 2*sqrt(3)
        val expected = sigma * sqrt(3.0)
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to theoretical mean (2 sigma sqrt(2 over pi))`() {
        // Theoretical mean for MaxwellBoltzmann(sigma=1) = 2*sigma*sqrt(2/pi) ≈ 1.5958
        val sigma = 1.0
        val theoreticalMean = 2.0 * sigma * sqrt(2.0 / PI)
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = MaxwellBoltzmannGenerator.create(sigma, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}