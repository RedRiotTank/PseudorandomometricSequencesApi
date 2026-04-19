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

/**
 * Mock basic for Apache Commons Math RandomGenerator.
 * Used for initialization tests where sample() is not called.
 */
class FakeCommonsRandomForPoisson : RandomGenerator {
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

class PoissonGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForPoisson()

    @Test
    fun `create should use default value (mean=1 0) when parameter is null`() {
        val generator = PoissonGenerator.create(null, fakeRandom)
        assertNotNull(generator)

        assertEquals(1.0, generator.distribution.mean)
    }

    @Test
    fun `create should use specified value when parameter is provided`() {
        val mean = 4.0
        val generator = PoissonGenerator.create(mean, fakeRandom)

        assertEquals(mean, generator.distribution.mean)
    }

    @ParameterizedTest(name = "PoissonGenerator should fail with mean: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if mean is not strictly positive`(mean: Double) {
        assertThrows<IllegalArgumentException> {
            PoissonGenerator.create(mean, fakeRandom)
        }
    }

    @Test
    fun `sample should return a deterministic valid discrete value (integer as double)`() {
        val deterministicRandom = JDKRandomGenerator()
        deterministicRandom.setSeed(42L)

        val mean = 4.0
        val generator = PoissonGenerator.create(mean, deterministicRandom)

        val sampleValue = generator.sample()

        assertTrue(sampleValue >= 0.0, "Poisson samples cannot be negative")

        assertEquals(0.0, sampleValue % 1.0, "Sample should be a discrete integer value represented as Double")
    }

    @Test
    fun `distribution name should be correct`() {
        assertEquals("poisson", PoissonGenerator.DISTRIBUTION_NAME)
    }
}