package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForBinomial : RandomGenerator {
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

class BinomialGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBinomial()


    @Test
    fun `create should use default values (trials=10 0, prob=0 5) when parameters are null`() {
        val generator = BinomialGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        assertEquals(10, generator.distribution.numberOfTrials)
        assertEquals(0.5, generator.distribution.probabilityOfSuccess)
    }

    @ParameterizedTest(name = "BinomialGenerator should fail with trials: {0} or probability: {1}")
    @CsvSource(
        "0.0, 0.5",     // Trials <= 0
        "1.5, 0.5",     // Trials !integer
        "10.0, -0.1",   // Probability < 0
        "10.0, 1.1",    // Probability > 1
        "0.5, 0.5"      // Trials !integer
    )
    fun `should throw exception for invalid parameters`(trials: Double, probability: Double) {
        assertThrows<IllegalArgumentException> {
            BinomialGenerator.create(trials, probability, fakeRandom)
        }
    }

    // --- sample method tests ---

    @Test
    fun `sample should delegate to internal distribution and return an integer Double`() {
        val generator = BinomialGenerator.create(10.0, 0.5, fakeRandom)
        val sampleValue = generator.sample()

        assertNotNull(sampleValue)
        assertTrue(sampleValue % 1.0 == 0.0)
    }
}
