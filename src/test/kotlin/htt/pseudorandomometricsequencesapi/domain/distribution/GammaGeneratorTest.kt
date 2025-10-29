package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForGamma : RandomGenerator {
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

class GammaGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForGamma()

    // --- create method tests ---

    @Test
    fun `create should use default values (shape=1 0, scale=1 0) when parameters are null`() {
        val generator = GammaGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // Gamma(1, 1) is equivalent to the Standard Exponential distribution Exp(1)
        assertEquals(1.0, generator.distribution.shape)
        assertEquals(1.0, generator.distribution.scale)
    }

    @ParameterizedTest(name = "GammaGenerator should fail with shape: {0} or scale: {1}")
    @CsvSource(
        "0.0, 1.0",     // Shape !positive
        "-1.0, 1.0",    // Shape negative
        "1.0, 0.0",     // Scale !positive
        "1.0, -5.0"     // Scale negative
    )
    fun `should throw exception if shape or scale are not positive`(shape: Double, scale: Double) {
        assertThrows<IllegalArgumentException> {
            GammaGenerator.create(shape, scale, fakeRandom)
        }
    }

    // --- sample method tests ---

    @Test
    fun `sample should delegate to internal distribution and return a Double`() {
        val generator = GammaGenerator.create(2.0, 3.0, fakeRandom)
        val sampleValue = generator.sample()

        assertNotNull(sampleValue)
        assertTrue(sampleValue is Double)
    }
}
