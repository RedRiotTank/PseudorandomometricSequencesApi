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
 * For IrwinHall with n=4, nextDouble always returns 0.5:
 * X = 4 * 0.5 = 2.0
 */
class FakeCommonsRandomForIrwinHall : RandomGenerator {
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

class IrwinHallGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForIrwinHall()

    @Test
    fun `create should use default value (n=12) when parameter is null`() {
        val generator = IrwinHallGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        // n=12, nextDouble=0.5 for all: X = 12 * 0.5 = 6.0
        assertEquals(6.0, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "IrwinHallGenerator should fail with n: {0}")
    @ValueSource(doubles = [0.0, -1.0, 1.5, 2.7])
    fun `should throw exception if n is not a positive integer`(n: Double) {
        assertThrows<IllegalArgumentException> {
            IrwinHallGenerator.create(n, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate sum of n uniform draws correctly with n=4`() {
        val generator = IrwinHallGenerator.create(4.0, fakeRandom)
        // n=4, nextDouble=0.5: X = 4 * 0.5 = 2.0
        assertEquals(2.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to theoretical mean (n over 2) for n=12`() {
        // Theoretical mean for IrwinHall(n=12) = n/2 = 6.0
        val theoreticalMean = 6.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = IrwinHallGenerator.create(12.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}