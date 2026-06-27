package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.apache.commons.math3.random.JDKRandomGenerator
import kotlin.math.abs

/**
 * For Slash with mu=0, sigma=1:
 * nextGaussian=1.0, nextDouble=0.5: X = 0 + 1 * 1.0/0.5 = 2.0
 */
class FakeCommonsRandomForSlash : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 1.0
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

class SlashGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForSlash()

    @Test
    fun `create should use default values (mu=0 0, sigma=1 0) when parameters are null`() {
        val generator = SlashGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // nextGaussian=1.0, nextDouble=0.5: X = 0 + 1 * 1.0/0.5 = 2.0
        assertEquals(2.0, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "SlashGenerator should fail with sigma: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if sigma is not positive`(sigma: Double) {
        assertThrows<IllegalArgumentException> {
            SlashGenerator.create(0.0, sigma, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = mu + sigma times Z over U correctly`() {
        val generator = SlashGenerator.create(0.0, 1.0, fakeRandom)
        // Z=1.0 (nextGaussian), U=0.5 (nextDouble): X = 0 + 1 * 1.0/0.5 = 2.0
        assertEquals(2.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample distribution with mu=0 should produce symmetric output (median near 0)`() {
        // Slash distribution has undefined mean due to extremely heavy tails (heavier than Cauchy).
        // We test symmetry via the median (which is defined) rather than the mean.
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = SlashGenerator.create(0.0, 1.0, rng)
        val N = 50_000
        val sorted = (1..N).map { generator.sample() }.sorted()
        val median = (sorted[N / 2 - 1] + sorted[N / 2]) / 2.0
        // Median of Slash(mu=0) should be near 0 (symmetric distribution)
        assertTrue(abs(median) < 0.1) {
            "Expected median close to 0.0, but was $median"
        }
    }
}