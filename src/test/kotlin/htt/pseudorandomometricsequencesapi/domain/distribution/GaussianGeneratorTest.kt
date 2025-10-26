package htt.pseudorandomometricsequencesapi.domain.distribution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Random

/** Mock for java.util.Random, ensuring nextGaussian() is deterministic. */
class FakeJavaRandomForGaussian : Random() {
    // nextGaussian() is overridden to provide a predictable value (Z=1.0)
    override fun nextGaussian() = 1.0

    // Minimal overrides to avoid potential issues in superclass constructor/methods
    override fun nextDouble() = 0.5
    override fun nextLong() = 1L
}

class GaussianGeneratorTest {

    private val fakeRandom = FakeJavaRandomForGaussian()

    @Test
    fun `create should use default values (mean=0 0, stddev=1 0) when parameters are null`() {
        val generator = GaussianGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)

        // Expected sample for mean=0.0, stddev=1.0, and Z=1.0: X = 0.0 + 1.0 * 1.0 = 1.0
        val expected = 1.0
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "GaussianGenerator should fail with stddev: {0}")
    @CsvSource("0.0", "-1.0") // Standard deviation positive (stddev > 0)
    fun `should throw exception if stddev is not positive`(stddev: Double) {
        // param1 (mean) can be anything, param2 (stddev) must be <= 0
        assertThrows<IllegalArgumentException> {
            GaussianGenerator.create(0.0, stddev, fakeRandom)
        }
    }

    @Test
    fun `sample should apply scaling and shifting (X = mu + sigma Z) correctly`() {
        val mean = 10.0
        val stddev = 5.0
        // Z = 1.0 (from fakeRandom.nextGaussian())

        val generator = GaussianGenerator.create(mean, stddev, fakeRandom)

        // Expected: X = 10.0 + 5.0 * 1.0 = 15.0
        val expected = 15.0

        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9) // Floating point precision check
    }
}
