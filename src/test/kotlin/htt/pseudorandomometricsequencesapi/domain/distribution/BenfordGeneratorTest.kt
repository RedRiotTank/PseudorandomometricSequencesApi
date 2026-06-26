package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * For Benford with nextDouble()=0.0:
 * X = floor(10^0) = floor(1) = 1.0
 */
class FakeCommonsRandomForBenford : RandomGenerator {
    override fun nextDouble() = 0.0
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

class BenfordGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBenford()

    @Test
    fun `create should produce a valid generator with no parameters`() {
        val generator = BenfordGenerator.create(fakeRandom)
        assertNotNull(generator)
    }

    @Test
    fun `sample should return 1 for nextDouble=0 0`() {
        val generator = BenfordGenerator.create(fakeRandom)
        // U=0.0: X = floor(10^0) = floor(1) = 1.0
        assertEquals(1.0, generator.sample(), 1e-9)
    }

    @Test
    fun `all samples should be in the range 1 to 9`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = BenfordGenerator.create(rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= 1.0 && it <= 9.0 && it % 1.0 == 0.0 }) {
            "All Benford samples must be integers in [1, 9]"
        }
    }

    @Test
    fun `sample mean should be close to theoretical Benford mean (approx 3 441)`() {
        // Benford mean = sum(k * log10(1 + 1/k), k=1..9) ≈ 3.441
        val theoreticalMean = (1..9).sumOf { k ->
            k * Math.log10(1.0 + 1.0 / k)
        }
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = BenfordGenerator.create(rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
