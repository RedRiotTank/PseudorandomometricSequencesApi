package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.exp

class FakeCommonsRandomForLogNormal : RandomGenerator {
    // nextGaussian is mocked to return 0.0, which means Z=0 in X = exp(mu + sigma * Z)
    override fun nextGaussian() = 0.0

    // Minimal overrides
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
}

class LogNormalGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForLogNormal()

    @Test
    fun `create should use default values (mu=0 0, sigma=1 0) when parameters are null`() {
        val generator = LogNormalGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)

        assertEquals(0.0, generator.mu)
        assertEquals(1.0, generator.sigma)
    }

    @ParameterizedTest(name = "LogNormalGenerator should fail with sigma: {1}")
    @CsvSource(
        "1.0, 0.0",     // Sigma !positive
        "1.0, -1.0"     // Sigma negative
    )
    fun `should throw exception if sigma is not positive`(mu: Double, sigma: Double) {
        assertThrows<IllegalArgumentException> {
            LogNormalGenerator.create(mu, sigma, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = exp(mu + sigma Z) correctly`() {
        val mu = 2.0
        val sigma = 0.5
        // Z = 0.0 (from fakeRandom.nextGaussian())

        val generator = LogNormalGenerator.create(mu, sigma, fakeRandom)

        // Expected: X = exp(2.0 + 0.5 * 0.0) = exp(2.0)
        val expected = exp(2.0)

        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9)
        assertTrue(sampleValue > 0.0)
    }
}
