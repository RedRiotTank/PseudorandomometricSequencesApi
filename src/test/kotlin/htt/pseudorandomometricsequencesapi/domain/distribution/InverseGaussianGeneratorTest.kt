package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForInverseGaussian : RandomGenerator {
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

class InverseGaussianGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForInverseGaussian()

    @Test
    fun `create should use default values (mu=1 0, lambda=1 0) when parameters are null`() {
        val generator = InverseGaussianGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        // With nextGaussian()=0.0, y=0, x = mu + 0 - 0 = mu = 1.0
        // u = nextDouble()=0.5, mean/(mean+x) = 1.0/2.0 = 0.5, so u<=0.5 is true, returns x=1.0
        val sample = generator.sample()
        assertTrue(sample > 0.0) { "Inverse Gaussian sample must be positive" }
    }

    @ParameterizedTest(name = "InverseGaussianGenerator should fail with mu: {0} or lambda: {1}")
    @CsvSource(
        "0.0, 1.0",    // mu <= 0
        "-1.0, 1.0",   // mu negative
        "1.0, 0.0",    // lambda <= 0
        "1.0, -1.0"    // lambda negative
    )
    fun `should throw exception for invalid parameters`(mu: Double, lambda: Double) {
        assertThrows<IllegalArgumentException> {
            InverseGaussianGenerator.create(mu, lambda, fakeRandom)
        }
    }

    @Test
    fun `sample should always return a positive value`() {
        val generator = InverseGaussianGenerator.create(1.0, 1.0, fakeRandom)
        val sample = generator.sample()
        assertTrue(sample > 0.0) { "Inverse Gaussian sample must be positive" }
    }

    @Test
    fun `sample mean should be close to mu`() {
        // Theoretical mean for InverseGaussian(mu=1.0, lambda=1.0) = mu = 1.0
        val mu = 1.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = InverseGaussianGenerator.create(mu, 1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in mu * 0.95..mu * 1.05) {
            "Expected mean near $mu but got $sampleMean"
        }
    }
}
