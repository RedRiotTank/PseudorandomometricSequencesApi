package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.ln

/**
 * Mock for Apache Commons Math RandomGenerator.
 *
 * nextDouble is set to 0.5.
 * A Chi-Squared distribution with k=2 degrees of freedom is mathematically equivalent
 * to an Exponential distribution with a mean of 2.
 * The inverse CDF for U=0.5 is: X = -2 * ln(1 - U) = -2 * ln(0.5) ≈ 1.386294361
 */
class FakeCommonsRandomForChiSquared : RandomGenerator {
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

class ChiSquaredGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForChiSquared()

    @Test
    fun `create should use default value (degreesOfFreedom=1 0) when parameter is null`() {
        val generator = ChiSquaredGenerator.create(null, fakeRandom)
        assertNotNull(generator)

        assertEquals(1.0, generator.distribution.degreesOfFreedom)
    }

    @Test
    fun `create should use specified value when parameter is provided`() {
        val degreesOfFreedom = 5.5
        val generator = ChiSquaredGenerator.create(degreesOfFreedom, fakeRandom)

        assertEquals(degreesOfFreedom, generator.distribution.degreesOfFreedom)
    }

    @ParameterizedTest(name = "ChiSquaredGenerator should fail with degreesOfFreedom: {0}")
    @ValueSource(doubles = [0.0, -1.0, -10.5])
    fun `should throw exception if degreesOfFreedom is not strictly positive`(degreesOfFreedom: Double) {
        assertThrows<IllegalArgumentException> {
            ChiSquaredGenerator.create(degreesOfFreedom, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate mathematically correct value for k=2 and U=0 5`() {
        val degreesOfFreedom = 2.0
        val generator = ChiSquaredGenerator.create(degreesOfFreedom, fakeRandom)

        // With k=2, equals to Exp(media=2). for U=0.5,expected value is -2 * ln(0.5)
        val expected = -2.0 * ln(0.5)
        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9)
    }

    @Test
    fun `distribution name should be correct`() {
        assertEquals("chi-squared", ChiSquaredGenerator.DISTRIBUTION_NAME)
    }
}