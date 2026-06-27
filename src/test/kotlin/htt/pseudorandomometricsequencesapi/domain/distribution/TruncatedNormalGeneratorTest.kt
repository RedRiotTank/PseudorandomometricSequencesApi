package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class FakeCommonsRandomForTruncatedNormal : RandomGenerator {
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

class TruncatedNormalGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForTruncatedNormal()

    @Test
    fun `create should use default values (mu=0, sigma=1, w=3) when parameters are null`() {
        val generator = TruncatedNormalGenerator.create(null, null, null, fakeRandom)
        assertNotNull(generator)
    }

    @ParameterizedTest(name = "TruncatedNormalGenerator should fail with sigma: {1} or w: {2}")
    @CsvSource(
        "0.0, 0.0, 3.0",   // sigma <= 0
        "0.0, -1.0, 3.0",  // sigma negative
        "0.0, 1.0, 0.0",   // w <= 0
        "0.0, 1.0, -1.0"   // w negative
    )
    fun `should throw exception for invalid parameters`(mu: Double, sigma: Double, w: Double) {
        assertThrows<IllegalArgumentException> {
            TruncatedNormalGenerator.create(mu, sigma, w, fakeRandom)
        }
    }

    @Test
    fun `samples should be within truncation bounds`() {
        // TruncatedNormal(mu=0, sigma=1, w=3): samples must be in [-3, 3]
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = TruncatedNormalGenerator.create(0.0, 1.0, 3.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= -3.0 && it <= 3.0 }) {
            "All Truncated Normal samples must be within [-3sigma, +3sigma]"
        }
    }

    @Test
    fun `sample mean should be close to mu=0 for symmetric truncation`() {
        // TruncatedNormal(mu=0, sigma=1, w=3) is symmetric around 0, so mean ≈ 0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = TruncatedNormalGenerator.create(0.0, 1.0, 3.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(abs(sampleMean) < 0.05) {
            "Expected mean near 0 but got $sampleMean"
        }
    }
}
