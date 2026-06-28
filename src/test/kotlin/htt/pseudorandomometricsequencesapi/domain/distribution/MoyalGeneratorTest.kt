package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.ln

class FakeCommonsRandomForMoyal : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class MoyalGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForMoyal()

    @Test fun `create should use default values (mu=0 0, sigma=1 0) when parameters are null`() {
        val gen = MoyalGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample().isFinite())
    }

    @ParameterizedTest(name = "Moyal should fail with sigma={1}")
    @CsvSource("0.0, 0.0", "0.0, -1.0")
    fun `should throw when scale is not positive`(mu: Double, sigma: Double) {
        assertThrows<IllegalArgumentException> { MoyalGenerator.create(mu, sigma, fakeRandom) }
    }

    @Test fun `all samples should be finite`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = MoyalGenerator.create(0.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample().isFinite()) }
    }

    @Test fun `statistical mean should be close to mu + sigma*(gamma_E + ln2) approx 1 2704`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu = 0.0; val sigma = 1.0
        val gen = MoyalGenerator.create(mu, sigma, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean = mu + sigma * (gamma_E + ln(2)) ≈ 0 + 1 * 1.2704
        assertEquals(1.2704, mean, 0.1)
    }

    @Test fun `distribution should have heavier right tail than left (positive skew)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = MoyalGenerator.create(0.0, 1.0, rng)
        val samples = (1..20_000).map { gen.sample() }.sorted()
        val p10 = samples[2000]
        val p90 = samples[18000]
        // Right tail should be much further from the median than the left tail
        val median = samples[10000]
        assertTrue((p90 - median) > (median - p10)) { "Right tail should be heavier than left tail" }
    }
}
