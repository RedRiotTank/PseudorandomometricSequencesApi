package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForZipf : RandomGenerator {
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

class ZipfGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForZipf()

    @Test
    fun `create should use default values (n=10, s=1 0) when parameters are null`() {
        val generator = ZipfGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertTrue(generator.distribution.numberOfElements == 10)
        assertTrue(generator.distribution.exponent == 1.0)
    }

    @ParameterizedTest(name = "ZipfGenerator should fail with n: {0} or s: {1}")
    @CsvSource(
        "0.0, 1.0",    // n <= 0
        "1.5, 1.0",    // n not integer
        "10.0, 0.0",   // s <= 0
        "10.0, -1.0"   // s negative
    )
    fun `should throw exception for invalid parameters`(n: Double, s: Double) {
        assertThrows<IllegalArgumentException> {
            ZipfGenerator.create(n, s, fakeRandom)
        }
    }

    @Test
    fun `sample should return an integer between 1 and n inclusive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = ZipfGenerator.create(10.0, 1.0, rng)
        val N = 1_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= 1.0 && it <= 10.0 && it % 1.0 == 0.0 }) {
            "All Zipf samples must be integers in [1, 10]"
        }
    }

    @Test
    fun `sample mean should be close to theoretical mean for n=10 s=1`() {
        // Zipf(n=10, s=1) mean ≈ 3.414 (Σ k*P(k) = Σ k^(1-s)/H(n,s), H(10,1)≈2.9290)
        val harmonicNumber10 = (1..10).sumOf { 1.0 / it }  // ≈ 2.9290
        val theoreticalMean = (1..10).sumOf { k -> k.toDouble() / harmonicNumber10 * (1.0 / k) }
        // Simplified: mean = Σ k*(k^(-1)/H) = Σ 1/H = n/H
        val theoreticalMeanSimple = 10.0 / harmonicNumber10
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = ZipfGenerator.create(10.0, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMeanSimple * 0.95..theoreticalMeanSimple * 1.05) {
            "Expected mean close to $theoreticalMeanSimple, but was $sampleMean"
        }
    }
}