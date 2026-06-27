package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.sqrt
import kotlin.math.PI

class FakeCommonsRandomForWeibull : RandomGenerator {
    // Minimal implementations
    override fun nextDouble() = 0.5
    override fun nextInt() = 1
    override fun nextInt(n: Int) = 1
    override fun nextLong() = 1L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = true
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
    override fun nextGaussian() = 0.0
}

class WeibullGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForWeibull()

    @Test
    fun `create should use default values (shape=1 0, scale=1 0) when parameters are null`() {
        val generator = WeibullGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)

        assertEquals(1.0, generator.distribution.shape)
        assertEquals(1.0, generator.distribution.scale)
    }

    // --- Validation tests (init block) ---

    @ParameterizedTest(name = "WeibullGenerator should fail with shape: {0} or scale: {1}")
    @CsvSource(
        "0.0, 1.0",     // Shape !positive
        "-1.0, 1.0",    // Shape negative
        "1.0, 0.0",     // Scale !positive
        "1.0, -5.0"     // Scale negative
    )
    fun `should throw exception if shape or scale are not positive`(shape: Double, scale: Double) {
        assertThrows<IllegalArgumentException> {
            WeibullGenerator.create(shape, scale, fakeRandom)
        }
    }

    @Test
    fun `sample should delegate to internal distribution and return a Double`() {
        val generator = WeibullGenerator.create(2.0, 3.0, fakeRandom)
        val sampleValue = generator.sample()

        assertNotNull(sampleValue)
        assertTrue(sampleValue is Double)
    }

    @Test
    fun `sample mean should be close to theoretical mean for shape=2 scale=1`() {
        // Theoretical mean for Weibull(shape=2, scale=1) = scale * Gamma(1 + 1/shape) = 1 * Gamma(1.5) = sqrt(pi)/2
        val theoreticalMean = sqrt(PI) / 2.0  // approx 0.8862
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = WeibullGenerator.create(2.0, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
