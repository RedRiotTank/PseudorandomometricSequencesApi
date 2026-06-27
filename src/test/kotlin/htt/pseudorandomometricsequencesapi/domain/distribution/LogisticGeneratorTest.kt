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
import kotlin.math.abs

class FakeCommonsRandomForLogistic : RandomGenerator {
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

class LogisticGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForLogistic()

    @Test
    fun `create should use default values (mu=0 0, s=1 0) when parameters are null`() {
        val generator = LogisticGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertEquals(0.0, generator.distribution.location)
        assertEquals(1.0, generator.distribution.scale)
    }

    @ParameterizedTest(name = "LogisticGenerator should fail with s: {1}")
    @CsvSource(
        "0.0, 0.0",    // s <= 0
        "0.0, -1.0"    // s negative
    )
    fun `should throw exception if s is not positive`(mu: Double, s: Double) {
        assertThrows<IllegalArgumentException> {
            LogisticGenerator.create(mu, s, fakeRandom)
        }
    }

    @Test
    fun `sample should return a Double`() {
        val generator = LogisticGenerator.create(0.0, 1.0, fakeRandom)
        assertNotNull(generator.sample())
    }

    @Test
    fun `sample mean should be close to mu=0 for default parameters`() {
        // Logistic(mu=0, s=1) has mean = mu = 0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = LogisticGenerator.create(0.0, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(abs(sampleMean) < 0.1) {
            "Expected sample mean close to 0.0, but was $sampleMean"
        }
    }
}
