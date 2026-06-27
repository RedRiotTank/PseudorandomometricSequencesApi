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

class FakeCommonsRandomForErlang : RandomGenerator {
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

class ErlangGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForErlang()

    @Test
    fun `create should use default values (k=1, lambda=1 0) when parameters are null`() {
        val generator = ErlangGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // Erlang(k=1, lambda=1) backed by Gamma(shape=1, scale=1/1=1)
        assertEquals(1.0, generator.distribution.shape)
        assertEquals(1.0, generator.distribution.scale)
    }

    @ParameterizedTest(name = "ErlangGenerator should fail with k: {0} or lambda: {1}")
    @CsvSource(
        "0.0, 1.0",    // k <= 0
        "1.5, 1.0",    // k not integer
        "2.7, 1.0",    // k not integer
        "1.0, 0.0",    // lambda <= 0
        "1.0, -1.0"    // lambda negative
    )
    fun `should throw exception for invalid parameters`(k: Double, lambda: Double) {
        assertThrows<IllegalArgumentException> {
            ErlangGenerator.create(k, lambda, fakeRandom)
        }
    }

    @Test
    fun `sample should return a positive Double`() {
        val generator = ErlangGenerator.create(3.0, 2.0, fakeRandom)
        val sample = generator.sample()
        assertNotNull(sample)
        assertTrue(sample > 0.0) { "Erlang sample must be positive" }
    }

    @Test
    fun `sample mean should be close to theoretical mean (k over lambda)`() {
        // Theoretical mean for Erlang(k=3, lambda=2) = k/lambda = 3/2 = 1.5
        val theoreticalMean = 1.5
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = ErlangGenerator.create(3.0, 2.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
