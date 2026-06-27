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

class FakeCommonsRandomForBeta : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextInt() = 1
    override fun nextInt(n: Int) = 1
    override fun nextLong() = 1L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = true
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
    override fun nextGaussian() = 0.0
}

class BetaGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBeta()


    @Test
    fun `create should use default values (alpha=1 0, beta=1 0) when parameters are null`() {
        val generator = BetaGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // Beta(1, 1) is the Continuous Uniform distribution on [0, 1]
        assertEquals(1.0, generator.distribution.alpha)
        assertEquals(1.0, generator.distribution.beta)
    }


    @ParameterizedTest(name = "BetaGenerator should fail with alpha: {0} or beta: {1}")
    @CsvSource(
        "0.0, 1.0",     // Alpha !positive
        "-1.0, 1.0",    // Alpha negative
        "1.0, 0.0",     // Beta !positive
        "1.0, -5.0"     // Beta negative
    )
    fun `should throw exception if alpha or beta are not positive`(alpha: Double, beta: Double) {
        assertThrows<IllegalArgumentException> {
            BetaGenerator.create(alpha, beta, fakeRandom)
        }
    }

    @Test
    fun `sample should delegate to internal distribution and return a Double`() {
        val generator = BetaGenerator.create(2.0, 2.0, fakeRandom)
        val sampleValue = generator.sample()

        assertNotNull(sampleValue)
        assertTrue(sampleValue is Double)
    }

    @Test
    fun `sample should be in (0, 1) and mean should be close to theoretical mean`() {
        // Theoretical mean for Beta(alpha=2, beta=2) = alpha / (alpha + beta) = 0.5
        val theoreticalMean = 0.5
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = BetaGenerator.create(2.0, 2.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it > 0.0 && it < 1.0 }) { "All Beta samples must be in (0, 1)" }
        val sampleMean = samples.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
