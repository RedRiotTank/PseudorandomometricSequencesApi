package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForLevy : RandomGenerator {
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

class LevyGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForLevy()

    @Test
    fun `create should use default values (mu=0 0, c=1 0) when parameters are null`() {
        val generator = LevyGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertEquals(0.0, generator.distribution.location)
        assertEquals(1.0, generator.distribution.scale)
    }

    @ParameterizedTest(name = "LevyGenerator should fail with c: {1}")
    @CsvSource(
        "0.0, 0.0",    // c <= 0
        "0.0, -1.0"    // c negative
    )
    fun `should throw exception if c is not positive`(mu: Double, c: Double) {
        assertThrows<IllegalArgumentException> {
            LevyGenerator.create(mu, c, fakeRandom)
        }
    }

    @Test
    fun `sample should return a value greater than location parameter`() {
        val generator = LevyGenerator.create(0.0, 1.0, fakeRandom)
        // Levy distribution has support [mu, infinity), all samples must be > mu=0
        val sample = generator.sample()
        assertNotNull(sample)
        assertTrue(sample > 0.0) { "Levy sample must be strictly positive (greater than mu)" }
    }
}