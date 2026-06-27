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

class FakeCommonsRandomForRice : RandomGenerator {
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

class RiceGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForRice()

    @Test
    fun `create should use default values (nu=0 0, sigma=1 0) when parameters are null`() {
        val generator = RiceGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
    }

    @ParameterizedTest(name = "RiceGenerator should fail with nu: {0} or sigma: {1}")
    @CsvSource(
        "-1.0, 1.0",   // nu negative
        "0.0, 0.0",    // sigma <= 0
        "0.0, -1.0"    // sigma negative
    )
    fun `should throw exception for invalid parameters`(nu: Double, sigma: Double) {
        assertThrows<IllegalArgumentException> {
            RiceGenerator.create(nu, sigma, fakeRandom)
        }
    }

    @Test
    fun `all samples should be positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = RiceGenerator.create(0.0, 1.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it > 0.0 }) { "All Rice samples must be positive" }
    }

    @Test
    fun `sample mean should be close to sqrt(pi over 2) for nu=0 sigma=1 (Rayleigh case)`() {
        // Rice(nu=0, sigma=1) reduces to Rayleigh(sigma=1), mean = sigma * sqrt(pi/2) ≈ 1.2533
        val theoreticalMean = sqrt(PI / 2.0)
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = RiceGenerator.create(0.0, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}