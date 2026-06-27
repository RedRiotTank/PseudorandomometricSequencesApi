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

class FakeCommonsRandomForGumbel : RandomGenerator {
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

class GumbelGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForGumbel()

    @Test
    fun `create should use default values (mu=0 0, beta=1 0) when parameters are null`() {
        val generator = GumbelGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertEquals(0.0, generator.distribution.location)
        assertEquals(1.0, generator.distribution.scale)
    }

    @ParameterizedTest(name = "GumbelGenerator should fail with beta: {1}")
    @CsvSource(
        "0.0, 0.0",    // beta <= 0
        "0.0, -1.0"    // beta negative
    )
    fun `should throw exception if beta is not positive`(mu: Double, beta: Double) {
        assertThrows<IllegalArgumentException> {
            GumbelGenerator.create(mu, beta, fakeRandom)
        }
    }

    @Test
    fun `sample should return a Double`() {
        val generator = GumbelGenerator.create(0.0, 1.0, fakeRandom)
        assertNotNull(generator.sample())
    }

    @Test
    fun `sample mean should be close to Euler-Mascheroni constant for default parameters`() {
        // Gumbel(mu=0, beta=1) has mean = mu + beta * euler_mascheroni ≈ 0.5772
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = GumbelGenerator.create(0.0, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        // Mean should be approx 0.5772 (Euler-Mascheroni constant)
        assertTrue(sampleMean in 0.45..0.70) {
            "Expected mean near 0.5772 (Euler-Mascheroni) but got $sampleMean"
        }
    }
}
