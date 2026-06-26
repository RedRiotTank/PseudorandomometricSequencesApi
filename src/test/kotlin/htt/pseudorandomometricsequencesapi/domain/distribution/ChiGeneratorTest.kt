package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.PI
import kotlin.math.sqrt

class FakeCommonsRandomForChi : RandomGenerator {
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

class ChiGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForChi()

    @Test
    fun `create should use default value (k=1 0) when parameter is null`() {
        val generator = ChiGenerator.create(null, fakeRandom)
        assertNotNull(generator)
    }

    @ParameterizedTest(name = "ChiGenerator should fail with k: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if k is not positive`(k: Double) {
        assertThrows<IllegalArgumentException> {
            ChiGenerator.create(k, fakeRandom)
        }
    }

    @Test
    fun `all samples should be positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = ChiGenerator.create(3.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it > 0.0 }) { "All Chi samples must be positive" }
    }

    @Test
    fun `sample mean should be close to theoretical mean for k=3`() {
        // Theoretical mean for Chi(k=3) = sqrt(2) * Gamma(2) / Gamma(1.5) = 2*sqrt(2/pi) ≈ 1.5958
        val theoreticalMean = 2.0 * sqrt(2.0 / PI)
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = ChiGenerator.create(3.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}