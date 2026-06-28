package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForCrystalBall : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class CrystalBallGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForCrystalBall()

    @Test fun `create should use default values (alpha=1 5, n=2 0) when parameters are null`() {
        val gen = CrystalBallGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "CrystalBall should fail with alpha={0}, n={1}")
    @CsvSource("0.0, 2.0", "-1.0, 2.0", "1.5, 1.0", "1.5, 0.5")
    fun `should throw for invalid parameters`(alpha: Double, n: Double) {
        assertThrows<IllegalArgumentException> { CrystalBallGenerator.create(alpha, n, fakeRandom) }
    }

    @Test fun `majority of samples should be in the Gaussian core above -alpha`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val alpha = 1.5; val n = 2.0
        val gen = CrystalBallGenerator.create(alpha, n, rng)
        val samples = (1..10_000).map { gen.sample() }
        val aboveNegAlpha = samples.count { it > -alpha }
        // Gaussian core area should dominate
        assertTrue(aboveNegAlpha > 5000)
    }

    @Test fun `should have power-law tail below -alpha`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = CrystalBallGenerator.create(1.5, 2.0, rng)
        val samples = (1..20_000).map { gen.sample() }
        // Some samples should be below -alpha (power-law tail)
        assertTrue(samples.any { it < -1.5 })
    }

    @Test fun `samples should be finite`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = CrystalBallGenerator.create(1.5, 2.0, rng)
        repeat(10_000) {
            assertTrue(gen.sample().isFinite())
        }
    }
}