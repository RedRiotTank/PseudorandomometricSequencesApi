package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForHypergeometric : RandomGenerator {
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

class HypergeometricGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForHypergeometric()

    @Test
    fun `create should use default values (N=100, K=50, n=10) when parameters are null`() {
        val generator = HypergeometricGenerator.create(null, null, null, fakeRandom)
        assertNotNull(generator)
        assertTrue(generator.distribution.populationSize == 100)
        assertTrue(generator.distribution.numberOfSuccesses == 50)
        assertTrue(generator.distribution.sampleSize == 10)
    }

    @ParameterizedTest(name = "HypergeometricGenerator should fail with N: {0}, K: {1}, n: {2}")
    @CsvSource(
        "0.0, 50.0, 10.0",     // N not positive
        "1.5, 50.0, 10.0",     // N not integer
        "100.0, -1.0, 10.0",   // K negative
        "100.0, 0.5, 10.0",    // K not integer
        "100.0, 101.0, 10.0",  // K > N
        "100.0, 50.0, 101.0"   // n > N
    )
    fun `should throw exception for invalid parameters`(n: Double, k: Double, s: Double) {
        assertThrows<IllegalArgumentException> {
            HypergeometricGenerator.create(n, k, s, fakeRandom)
        }
    }

    @Test
    fun `sample should return a non-negative integer Double`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = HypergeometricGenerator.create(100.0, 50.0, 10.0, rng)
        val N = 1_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= 0.0 && it % 1.0 == 0.0 }) {
            "All Hypergeometric samples must be non-negative integers"
        }
    }

    @Test
    fun `sample mean should be close to theoretical mean (n times K over N)`() {
        // Hypergeometric(N=100, K=50, n=10) mean = n*K/N = 10*50/100 = 5.0
        val theoreticalMean = 5.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = HypergeometricGenerator.create(100.0, 50.0, 10.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
