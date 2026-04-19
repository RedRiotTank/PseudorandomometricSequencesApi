package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.sqrt

/**
 * Mock for Apache Commons Math RandomGenerator.
 *
 * nextDouble is set to 0.5.
 * The inverse CDF for Pareto is X = scale / (1 - U)^(1/shape).
 * With scale=1, shape=2, and U=0.5: X = 1 / (0.5)^(0.5) = 1 / sqrt(0.5) = sqrt(2) ≈ 1.41421356
 */
class FakeCommonsRandomForPareto : RandomGenerator {
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

class ParetoGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForPareto()

    @Test
    fun `create should use default values (scale=1 0, shape=1 0) when parameters are null`() {
        val generator = ParetoGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)

        assertEquals(1.0, generator.distribution.scale)
        assertEquals(1.0, generator.distribution.shape)
    }

    @Test
    fun `create should use specified values when parameters are provided`() {
        val scale = 5.0
        val shape = 3.5
        val generator = ParetoGenerator.create(scale, shape, fakeRandom)

        assertEquals(scale, generator.distribution.scale)
        assertEquals(shape, generator.distribution.shape)
    }

    @ParameterizedTest(name = "ParetoGenerator should fail with scale:{0}, shape:{1}")
    @CsvSource(
        "0.0, 2.0",
        "-1.0, 2.0",
        "1.0, 0.0",
        "1.0, -1.0"
    )
    fun `should throw exception for invalid parameters`(scale: Double, shape: Double) {
        assertThrows<IllegalArgumentException> {
            ParetoGenerator.create(scale, shape, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate mathematically correct value based on inverse CDF`() {
        val scale = 1.0
        val shape = 2.0

        val generator = ParetoGenerator.create(scale, shape, fakeRandom)

        // Expected: sqrt(2)
        val expected = sqrt(2.0)
        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9)
    }

    @Test
    fun `distribution name should be correct`() {
        assertEquals("pareto", ParetoGenerator.DISTRIBUTION_NAME)
    }
}