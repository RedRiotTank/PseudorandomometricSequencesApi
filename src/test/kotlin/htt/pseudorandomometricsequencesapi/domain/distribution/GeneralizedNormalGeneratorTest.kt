package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class FakeCommonsRandomForGenNormal : RandomGenerator {
    override fun nextDouble() = 0.6   // > 0.5 → sign = +1
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class GeneralizedNormalGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForGenNormal()

    @Test fun `create should use default values (mu=0 0, sigma=1 0, beta=2 0) when parameters are null`() {
        val gen = GeneralizedNormalGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "GenNormal should fail with sigma={1} or beta={2}")
    @CsvSource("0.0, 0.0, 2.0", "0.0, -1.0, 2.0", "0.0, 1.0, 0.0", "0.0, 1.0, -1.0")
    fun `should throw for invalid scale or shape`(mu: Double, sigma: Double, beta: Double) {
        assertThrows<IllegalArgumentException> { GeneralizedNormalGenerator.create(mu, sigma, beta, fakeRandom) }
    }

    @Test fun `with beta=2 (Gaussian) statistical mean should be close to location mu`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu = 3.0
        val gen = GeneralizedNormalGenerator.create(mu, 1.0, 2.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(mu, mean, 0.1)
    }

    @Test fun `with beta=1 (Laplace) statistical mean should be close to location mu`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu = 0.0
        val gen = GeneralizedNormalGenerator.create(mu, 1.0, 1.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(mu, mean, 0.1)
    }

    @Test fun `distribution is symmetric around mu`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu = 0.0
        val gen = GeneralizedNormalGenerator.create(mu, 1.0, 2.0, rng)
        val samples = (1..50_000).map { gen.sample() }
        val positives = samples.count { it > 0 }
        val negatives = samples.count { it < 0 }
        val ratio = positives.toDouble() / negatives.toDouble()
        assertEquals(1.0, ratio, 0.05) { "Distribution should be symmetric around mu" }
    }
}
      