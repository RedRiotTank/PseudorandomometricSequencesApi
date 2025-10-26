package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Mock for Apache Commons Math RandomGenerator.
 *
 * nextDouble is set to 0.75, which for the Cauchy distribution's inverse CDF
 * (X = x0 + gamma * tan(pi * (U - 0.5))), results in:
 * X = x0 + gamma * tan(pi * (0.75 - 0.5))
 * X = x0 + gamma * tan(pi/4)
 * X = x0 + gamma * 1
 */
class FakeCommonsRandomForCauchy : RandomGenerator {
    override fun nextDouble() = 0.75
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

class CauchyGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForCauchy()

    @Test
    fun `create should use default values (location=0 0, scale=1 0) when parameters are null`() {
        val generator = CauchyGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)

        assertEquals(0.0, generator.location)
        assertEquals(1.0, generator.scale)
    }

    @Test
    fun `create should use specified values when parameters are provided`() {
        val location = 5.5
        val scale = 0.25
        val generator = CauchyGenerator.create(location, scale, fakeRandom)

        assertEquals(location, generator.location)
        assertEquals(scale, generator.scale)
    }


    @ParameterizedTest(name = "CauchyGenerator should fail with scale: {1}")
    @CsvSource(
        "1.0, 0.0",     // Scale !strictly-positive
        "1.0, -1.0"     // Scale negative
    )
    fun `should throw exception if scale is not positive`(location: Double, scale: Double) {
        assertThrows<IllegalArgumentException> {
            CauchyGenerator.create(location, scale, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = location + scale correctly when U equals 0 75`() {
        val location = 10.0
        val scale = 5.0

        val generator = CauchyGenerator.create(location, scale, fakeRandom)

        // Expected: X = 10.0 + 5.0 * tan(pi/4) = 15.0
        val expected = location + scale

        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9)
    }

    @Test
    fun `distribution name should be correct`() {
        assertEquals("cauchy", CauchyGenerator.DISTRIBUTION_NAME)
    }
}
