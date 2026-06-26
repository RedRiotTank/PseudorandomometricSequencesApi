package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForDiscreteUniform : RandomGenerator {
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

class DiscreteUniformGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForDiscreteUniform()

    @Test
    fun `create should use default values (a=0, b=9) when parameters are null`() {
        val generator = DiscreteUniformGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertTrue(generator.distribution.supportLowerBound == 0)
        assertTrue(generator.distribution.supportUpperBound == 9)
    }

    @ParameterizedTest(name = "DiscreteUniformGenerator should fail with a: {0} or b: {1}")
    @CsvSource(
        "0.5, 9.0",    // a not integer
        "0.0, 9.5",    // b not integer
        "5.0, 3.0"     // b < a
    )
    fun `should throw exception for invalid parameters`(a: Double, b: Double) {
        assertThrows<IllegalArgumentException> {
            DiscreteUniformGenerator.create(a, b, fakeRandom)
        }
    }

    @Test
    fun `sample should return an integer Double within lower and upper bounds`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = DiscreteUniformGenerator.create(0.0, 9.0, rng)
        val N = 1_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= 0.0 && it <= 9.0 && it % 1.0 == 0.0 }) {
            "All Discrete Uniform samples must be integers in [0, 9]"
        }
    }

    @Test
    fun `sample mean should be close to (a + b) over 2`() {
        // DiscreteUniform(a=0, b=9) mean = (0+9)/2 = 4.5
        val theoreticalMean = 4.5
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = DiscreteUniformGenerator.create(0.0, 9.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean close to $theoreticalMean, but was $sampleMean"
        }
    }
}