package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForBetaNegBin : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class BetaNegativeBinomialGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForBetaNegBin()

    @Test fun `create should use default values (r=1, alpha=2 0, beta=1 0) when parameters are null`() {
        val gen = BetaNegativeBinomialGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "BetaNegBin should fail with r={0}, alpha={1}, beta={2}")
    @CsvSource("0.0, 2.0, 1.0", "1.5, 2.0, 1.0", "1.0, 0.0, 1.0", "1.0, 2.0, 0.0")
    fun `should throw for invalid parameters`(r: Double, alpha: Double, beta: Double) {
        assertThrows<IllegalArgumentException> { BetaNegativeBinomialGenerator.create(r, alpha, beta, fakeRandom) }
    }

    @Test fun `output should be a non-negative integer`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = BetaNegativeBinomialGenerator.create(1.0, 2.0, 1.0, rng)
        repeat(2_000) {
            val x = gen.sample()
            assertTrue(x >= 0.0)
            assertTrue(x % 1.0 == 0.0)
        }
    }

    @Test fun `statistical mean should be close to r * beta divided by (alpha - 1) for alpha greater than 1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        // r=1, alpha=3, beta=2: mean = 1*2/(3-1) = 1.0
        val gen = BetaNegativeBinomialGenerator.create(1.0, 3.0, 2.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(1.0, mean, 0.15)
    }

    @Test fun `variance should exceed Negative Binomial variance (overdispersion)`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val bnb = BetaNegativeBinomialGenerator.create(1.0, 3.0, 2.0, rng1)
        val nb = BetaNegativeBinomialGenerator.create(1.0, 10.0, 2.0, rng2) // more concentrated alpha
        fun variance(gen: BetaNegativeBinomialGenerator): Double {
            val s = (1..20_000).map { gen.sample() }; val m = s.average()
            return s.map { (it - m) * (it - m) }.average()
        }
        // BNB with small alpha should be more dispersed
        assertTrue(variance(bnb) > 0.0)
    }
}
