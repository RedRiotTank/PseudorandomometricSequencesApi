package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.sqrt

class FakeCommonsRandomForFoldedNormal : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = -1.0   // abs(-1) = 1
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class FoldedNormalGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForFoldedNormal()

    @Test fun `create should use default values (mu=0 0, sigma=1 0) when parameters are null`() {
        val gen = FoldedNormalGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // |N(0,1)| with Z=-1: |0 + 1*(-1)| = 1.0
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "FoldedNormal should fail with sigma={1}")
    @CsvSource("0.0, 0.0", "0.0, -1.0")
    fun `should throw when sigma is not positive`(mu: Double, sigma: Double) {
        assertThrows<IllegalArgumentException> { FoldedNormalGenerator.create(mu, sigma, fakeRandom) }
    }

    @Test fun `sample formula abs(N(mu, sigma)) is correct`() {
        // nextGaussian=-1, mu=0, sigma=1: |0+1*(-1)| = 1.0
        val gen = FoldedNormalGenerator.create(0.0, 1.0, fakeRandom)
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @Test fun `all samples should be non-negative`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = FoldedNormalGenerator.create(0.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample() >= 0.0) }
    }

    @Test fun `with mu=0 statistical mean should be close to sigma * sqrt(2 div pi)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val sigma = 1.0
        val gen = FoldedNormalGenerator.create(0.0, sigma, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical: sigma * sqrt(2/π) ≈ 0.7979
        val theoreticalMean = sigma * sqrt(2.0 / kotlin.math.PI)
        assertEquals(theoreticalMean, mean, 0.05)
    }
}