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

class FakeCommonsRandomForBernoulli : RandomGenerator {
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

class BernoulliGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBernoulli()

    @Test
    fun `create should use default value (p=0 5) when parameter is null`() {
        val generator = BernoulliGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        assertEquals(1, generator.distribution.numberOfTrials)
        assertEquals(0.5, generator.distribution.probabilityOfSuccess)
    }

    @ParameterizedTest(name = "BernoulliGenerator should fail with p: {0}")
    @ValueSource(doubles = [-0.1, 1.1, -5.0, 2.0])
    fun `should throw exception if p is outside the valid range 0 to 1`(p: Double) {
        assertThrows<IllegalArgumentException> {
            BernoulliGenerator.create(p, fakeRandom)
        }
    }

    @Test
    fun `sample should return either 0 or 1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = BernoulliGenerator.create(0.5, rng)
        val N = 1_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it == 0.0 || it == 1.0 }) {
            "All Bernoulli samples must be 0 or 1"
        }
    }

    @Test
    fun `sample mean should be close to p`() {
        // Theoretical mean for Bernoulli(p=0.5) = p = 0.5
        val p = 0.5
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = BernoulliGenerator.create(p, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in p * 0.95..p * 1.05) {
            "Expected mean near $p but got $sampleMean"
        }
    }
}
