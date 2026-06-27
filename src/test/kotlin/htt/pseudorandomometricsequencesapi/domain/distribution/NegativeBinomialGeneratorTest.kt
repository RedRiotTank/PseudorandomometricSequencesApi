package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForNegativeBinomial : RandomGenerator {
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

class NegativeBinomialGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForNegativeBinomial()

    @Test
    fun `create should use default values (r=1, p=0 5) when parameters are null`() {
        val generator = NegativeBinomialGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertTrue(generator.distribution.numberOfSuccesses == 1)
        assertTrue(generator.distribution.probabilityOfSuccess == 0.5)
    }

    @ParameterizedTest(name = "NegativeBinomialGenerator should fail with r: {0} or p: {1}")
    @CsvSource(
        "0.0, 0.5",    // r <= 0
        "1.5, 0.5",    // r not integer
        "1.0, 0.0",    // p <= 0
        "1.0, 1.1",    // p > 1
        "1.0, -0.1"    // p negative
    )
    fun `should throw exception for invalid parameters`(r: Double, p: Double) {
        assertThrows<IllegalArgumentException> {
            NegativeBinomialGenerator.create(r, p, fakeRandom)
        }
    }

    @Test
    fun `sample should return a non-negative integer Double`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = NegativeBinomialGenerator.create(1.0, 0.5, rng)
        val N = 1_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= 0.0 && it % 1.0 == 0.0 }) {
            "All Negative Binomial samples must be non-negative integers"
        }
    }

    @Test
    fun `sample mean should be close to theoretical mean for r=1 p=0 5`() {
        // NegativeBinomial(r=1, p=0.5) mean = r * (1-p) / p = 1 * 0.5 / 0.5 = 1.0
        val theoreticalMean = 1.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = NegativeBinomialGenerator.create(1.0, 0.5, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
