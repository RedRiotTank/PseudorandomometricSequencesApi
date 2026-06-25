package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

class FakeCommonsRandomForRayleigh : RandomGenerator {
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

class RayleighGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForRayleigh()

    @Test
    fun `create should use default value (sigma=1 0) when parameter is null`() {
        val generator = RayleighGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        // Default sigma=1.0, U=0.5: X = 1.0 * sqrt(-2 * ln(0.5))
        val expected = sqrt(-2.0 * ln(0.5))
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "RayleighGenerator should fail with sigma: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if sigma is not positive`(sigma: Double) {
        assertThrows<IllegalArgumentException> {
            RayleighGenerator.create(sigma, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = sigma times sqrt(-2 times ln(U)) correctly`() {
        val sigma = 2.0
        val generator = RayleighGenerator.create(sigma, fakeRandom)
        // U = 0.5 from fakeRandom: X = 2.0 * sqrt(-2 * ln(0.5))
        val expected = sigma * sqrt(-2.0 * ln(0.5))
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to theoretical mean (sigma times sqrt(pi over 2))`() {
        // Theoretical mean for Rayleigh(sigma=1) = sigma * sqrt(pi/2) ≈ 1.2533
        val sigma = 1.0
        val theoreticalMean = sigma * sqrt(PI / 2.0)
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = RayleighGenerator.create(sigma, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expect