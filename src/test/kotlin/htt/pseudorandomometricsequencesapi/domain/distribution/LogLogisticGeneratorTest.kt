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
import kotlin.math.PI
import kotlin.math.sin

/**
 * For LogLogistic with alpha=1, beta=1:
 * X = alpha * (U/(1-U))^(1/beta) = 1 * (0.5/0.5)^1 = 1.0
 */
class FakeCommonsRandomForLogLogistic : RandomGenerator {
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

class LogLogisticGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForLogLogistic()

    @Test
    fun `create should use default values (alpha=1 0, beta=1 0) when parameters are null`() {
        val generator = LogLogisticGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // alpha=1, beta=1, U=0.5: X = 1 * (0.5/0.5)^1 = 1.0
        assertEquals(1.0, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "LogLogisticGenerator should fail with alpha: {0} or beta: {1}")
    @CsvSource(
        "0.0, 1.0",    // alpha <= 0
        "-1.0, 1.0",   // alpha negative
        "1.0, 0.0",    // beta <= 0
        "1.0, -1.0"    // beta negative
    )
    fun `should throw exception for invalid parameters`(alpha: Double, beta: Double) {
        assertThrows<IllegalArgumentException> {
            LogLogisticGenerator.create(alpha, beta, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = alpha times (U over (1-U)) to the power 1 over beta correctly`() {
        val generator = LogLogisticGenerator.create(1.0, 1.0, fakeRandom)
        // U=0.5: X = 1 * (0.5/0.5)^(1/1) = 1.0
        assertEquals(1.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to theoretical mean for beta=2 alpha=1`() {
        // Mean for LogLogistic(alpha=1, beta=2) = pi*alpha / (beta*sin(pi/beta)) = pi / (2*sin(pi/2)) = pi/2 ≈ 1.5708
        val theoreticalMean = PI / 2.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = LogLogisticGenerator.create(1.0, 2.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Ex