package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForLandau : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class LandauGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForLandau()

    @Test fun `create should use default values (mu=0 0, sigma=1 0) when parameters are null`() {
        val gen = LandauGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample().isFinite())
    }

    @ParameterizedTest(name = "Landau should fail with sigma={1}")
    @CsvSource("0.0, 0.0", "0.0, -1.0")
    fun `should throw when scale is not positive`(mu: Double, sigma: Double) {
        assertThrows<IllegalArgumentException> { LandauGenerator.create(mu, sigma, fakeRandom) }
    }

    @Test fun `all samples should be finite`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = LandauGenerator.create(0.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample().isFinite()) }
    }

    @Test fun `distribution should have heavy right tail much larger than median`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = LandauGenerator.create(0.0, 1.0, rng)
        val samples = (1..10_000).map { gen.sample() }.sorted()
        val median = samples[5000]
        val p99    = samples[9900]
        // Landau has very heavy right tail
        assertTrue(p99 > median + 10.0)
    }

    @Test fun `location shift should move distribution`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen0 = LandauGenerator.create(0.0, 1.0, rng1)
        val gen5 = LandauGenerator.create(5.0, 1.0, rng2)
        val mean0 = (1..5000).map { gen0.sample() }.average()
        val mean5 = (1..5000).map { gen5.sample() }.average()
        assertEquals(mean0 + 5.0, mean5, 1e-5)
    }
}