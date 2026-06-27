package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForBetaBinomial : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0
    override fun nextInt(n: Int) = 0
    override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
}

class BetaBinomialGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBetaBinomial()

    @Test
    fun `create should use default values (n=10, alpha=1 0, beta=1 0) when parameters are null`() {
        val gen = BetaBinomialGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "BetaBinomial should fail with n={0}, alpha={1}, beta={2}")
    @CsvSource(
        "0.0, 1.0, 1.0",
        "1.5, 1.0, 1.0",
        "10.0, 0.0, 1.0",
        "10.0, -1.0, 1.0",
        "10.0, 1.0, 0.0",
        "10.0, 1.0, -1.0"
    )
    fun `should throw for invalid parameters`(n: Double, alpha: Double, beta: Double) {
        assertThrows<IllegalArgumentException> { BetaBinomialGenerator.create(n, alpha, beta, fakeRandom) }
    }

    @Test
    fun `output should be a non-negative integer not exceeding n`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val n = 10
        val gen = BetaBinomialGenerator.create(n.toDouble(), 2.0, 2.0, rng)
        repeat(5_000) {
            val x = gen.sample()
            assertTrue(x >= 0.0 && x <= n.toDouble())
            assertTrue(x % 1.0 == 0.0)
        }
    }

    @Test
    fun `statistical mean should be close to n * alpha divided by (alpha + beta)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val n = 10.0; val alpha = 2.0; val beta = 2.0
        val gen = BetaBinomialGenerator.create(n, alpha, beta, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean = n * alpha / (alpha + beta) = 10 * 2 / 4 = 5.0
        assertEquals(5.0, mean, 0.2)
    }

    @Test
    fun `variance should exceed binomial variance (overdispersion property)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val n = 10; val alpha = 1.0; val beta = 1.0
        val gen = BetaBinomialGenerator.create(n.toDouble(), alpha, beta, rng)
        val samples = (1..50_000).map { gen.sample() }
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()
        // Binomial(10, 0.5) variance = 2.5; BetaBinomial should be larger
        assertTrue(variance > 2.5)
    }
}
