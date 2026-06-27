package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.PI
import kotlin.math.sqrt

class FakeCommonsRandomForNakagami : RandomGenerator {
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

class NakagamiGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForNakagami()

    @Test
    fun `create should use default values (m=1 0, omega=1 0) when parameters are null`() {
        val generator = NakagamiGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertTrue(generator.distribution.shape == 1.0) { "Default m should be 1.0" }
        assertTrue(generator.distribution.scale == 1.0) { "Default omega should be 1.0" }
    }

    @ParameterizedTest(name = "NakagamiGenerator should fail with m: {0} or omega: {1}")
    @CsvSource(
        "0.4, 1.0",    // m < 0.5
        "0.0, 1.0",    // m = 0
        "1.0, 0.0",    // omega <= 0
        "1.0, -1.0"    // omega negative
    )
    fun `should throw exception for invalid parameters`(m: Double, omega: Double) {
        assertThrows<IllegalArgumentException> {
            NakagamiGenerator.create(m, omega, fakeRandom)
        }
    }

    @Test
    fun `all samples should be positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = NakagamiGenerator.create(1.0, 1.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it > 0.0 }) { "All Nakagami samples must be positive" }
    }

    @Test
    fun `sample mean should be close to theoretical mean for m=1 omega=1`() {
        // Theoretical mean for Nakagami(m=1, omega=1) = Gamma(m + 0.5)/Gamma(m) * sqrt(omega/m)
        // For m=1: = Gamma(1.5)/Gamma(1) * sqrt(1/1) = (sqrt(pi)/2) / 1 = sqrt(pi)/2 ≈ 0.8862
        val theoreticalMean = sqrt(PI) / 2.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = NakagamiGenerator.create(1.0, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}