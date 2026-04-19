package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Mock for Apache Commons Math RandomGenerator.
 *
 * nextDouble is set to 0.125.
 * For a Triangular distribution where a=0, c=5, b=10, the threshold is (c-a)/(b-a) = 0.5.
 * Since U (0.125) < 0.5, the inverse CDF is: X = a + sqrt(U * (b - a) * (c - a))
 * X = 0 + sqrt(0.125 * 10 * 5) = sqrt(6.25) = 2.5
 */
class FakeCommonsRandomForTriangular : RandomGenerator {
    override fun nextDouble() = 0.125
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

class TriangularGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForTriangular()

    @Test
    fun `create should use default values (min=0 0, mode=0 5, max=1 0) when parameters are null`() {
        val generator = TriangularGenerator.create(null, null, null, fakeRandom)
        assertNotNull(generator)

        assertEquals(0.0, generator.distribution.supportLowerBound)
        assertEquals(0.5, generator.distribution.mode)
        assertEquals(1.0, generator.distribution.supportUpperBound)
    }

    @Test
    fun `create should use specified values when parameters are provided`() {
        val min = 10.0
        val mode = 15.0
        val max = 30.0

        val generator = TriangularGenerator.create(min, mode, max, fakeRandom)

        assertEquals(min, generator.distribution.supportLowerBound)
        assertEquals(mode, generator.distribution.mode)
        assertEquals(max, generator.distribution.supportUpperBound)
    }

    @ParameterizedTest(name = "TriangularGenerator should fail with min:{0}, mode:{1}, max:{2}")
    @CsvSource(
        "10.0, 15.0, 5.0",   // Error: max < min
        "10.0, 5.0,  30.0",  // Error: mode < min
        "10.0, 35.0, 30.0",  // Error: mode > max
        "10.0, 15.0, 10.0"   // Error: min == max
    )
    fun `should throw exception for invalid parameters`(min: Double, mode: Double, max: Double) {
        assertThrows<IllegalArgumentException> {
            TriangularGenerator.create(min, mode, max, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate correct value mathematically based on inverse CDF`() {
        val min = 0.0
        val mode = 5.0
        val max = 10.0

        val generator = TriangularGenerator.create(min, mode, max, fakeRandom)

        // Expected: X = 0.0 + sqrt(0.125 * (10.0 - 0.0) * (5.0 - 0.0)) = 2.5
        val expected = 2.5

        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9)
    }

    @Test
    fun `distribution name should be correct`() {
        assertEquals("triangular", TriangularGenerator.DISTRIBUTION_NAME)
    }
}