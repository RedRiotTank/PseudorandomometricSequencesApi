package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.ln

class FakeCommonsRandomForLogarithmic : RandomGenerator {
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

class LogarithmicGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForLogarithmic()

    @Test
    fun `create should use default value (p=0 5) when parameter is null`() {
        val generator = LogarithmicGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        val sample = generator.sample()
        assertTrue(sample > 0.0 && sample % 1.0 == 0.0) {
            "Logarithmic sample must be a positive integer, got $sample"
        }
    }

    @ParameterizedTest(name = "LogarithmicGenerator should fail with p: {0}")
    @ValueSource(doubles = [0.0, 1.0, -0.5, 1.5])
    fun `should throw exception if p is not in open interval (0, 1)`(p: Double) {
        assertThrows<IllegalArgumentException> {
            LogarithmicGenerator.create(p, fakeRandom)
        }
    }

    @Test
    fun `sample should return a positive integer`() {
        val generator = LogarithmicGenerator.create(0.5, fakeRandom)
        val sample = generator.sample()
        assertTrue(sample > 0.0) { "Logarithmic sample must be positive, got $sample" }
        assertTrue(sample % 1.0 == 0.0) { "Logarithmic sample must be an integer, got $sample" }
    }

    @Test
    fun `sample mean should be close to theoretical mean for p=0 5`() {
        // Theoretical mean for Logarithmic(p=0.5) = -p / ((1-p) * ln(1-p)) = -0.5 / (0.5 * ln(0.5)) = 1/ln2 ≈ 1.4427
        val p = 0.5
        val theoreticalMean = -p / ((1.0 - p) * ln(1.0 - p))
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = LogarithmicGenerator.create(p, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}