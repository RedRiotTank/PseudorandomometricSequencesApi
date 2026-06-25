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

class FakeCommonsRandomForFDistribution : RandomGenerator {
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

class FDistributionGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForFDistribution()

    @Test
    fun `create should use default values (d1=5 0, d2=5 0) when parameters are null`() {
        val generator = FDistributionGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertEquals(5.0, generator.distribution.numeratorDegreesOfFreedom)
        assertEquals(5.0, generator.distribution.denominatorDegreesOfFreedom)
    }

    @ParameterizedTest(name = "FDistributionGenerator should fail with d1: {0} or d2: {1}")
    @CsvSource(
        "0.0, 5.0",    // d1 <= 0
        "-1.0, 5.0",   // d1 negative
        "5.0, 0.0",    // d2 <= 0
        "5.0, -1.0"    // d2 negative
    )
    fun `should throw exception for invalid parameters`(d1: Double, d2: Double) {
        assertThrows<IllegalArgumentException> {
            FDistributionGenerator.create(d1, d2, fakeRandom)
        }
    }

    @Test
    fun `sample should return a positive Double`() {
        val generator = FDistributionGenerator.create(5.0, 10.0, fakeRandom)
        val sample = generator.sample()
        assertNotNull(sample)
        assertTrue(sample > 0.0) { "F-distribution sample must be positive" }
    }

    @Test
    fun `sample mean should be close to theoretical mean d2 over (d2-2) for d1=5 d2=10`() {
        // Mean for F(d1=5, d2=10) = d2 / (d2-2) = 10/8 = 1.25
        val theoreticalMean = 1.25
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = FDistributionGenerator.create(5.0, 10.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expecte