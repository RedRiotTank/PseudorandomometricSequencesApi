package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.ln

/**
 * For GEV with xi=0 (Gumbel), inverse CDF: X = mu - sigma * ln(-ln(U))
 * With mu=0, sigma=1, U=0.5: X = 0 - 1 * ln(-ln(0.5)) = -ln(ln2) ≈ 0.3665
 */
class FakeCommonsRandomForGev : RandomGenerator {
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

class GevGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForGev()

    @Test
    fun `create should use default values (mu=0, sigma=1, xi=0) when parameters are null`() {
        val generator = GevGenerator.create(null, null, null, fakeRandom)
        assertNotNull(generator)
        // With xi=0 (Gumbel), U=0.5: X = 0 - 1 * ln(-ln(0.5)) ≈ 0.3665
        val expected = 0.0 - 1.0 * ln(-ln(0.5))
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "GevGenerator should fail with sigma: {1}")
    @CsvSource(
        "0.0, 0.0, 0.0",   // sigma <= 0
        "0.0, -1.0, 0.0"   // sigma negative
    )
    fun `should throw exception if sigma is not positive`(mu: Double, sigma: Double, xi: Double) {
        assertThrows<IllegalArgumentException> {
            GevGenerator.create(mu, sigma, xi, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate correct value for xi=0 (Gumbel case)`() {
        val generator = GevGenerator.create(0.0, 1.0, 0.0, fakeRandom)
        // U=0.5, xi=0: X = 0 - 1 * ln(-ln(0.5))
        val expected = -ln(-ln(0.5))
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to Euler-Mascheroni constant for default Gumbel parameters`() {
        // GEV(mu=0, sigma=1, xi=0) is Gumbel. Mean = mu + sigma * gamma ≈ 0 + 0.5772 = 0.5772
        val theoreticalMean = 0.5772  // Euler-Mascheroni constant
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = GevGenerator.create(0.0, 1.0, 0.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in 0.45..0.70) {
            "Expected mean near $theoreticalMean, but was $sampleMean"
        }
    }
}