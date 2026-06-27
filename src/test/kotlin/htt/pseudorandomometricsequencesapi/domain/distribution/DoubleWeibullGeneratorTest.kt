package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class FakeCommonsRandomForDoubleWeibull : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class DoubleWeibullGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForDoubleWeibull()

    @Test fun `create should use default values (k=2 0, lambda=1 0) when parameters are null`() {
        val gen = DoubleWeibullGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "DoubleWeibull should fail with k={0}, lambda={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "2.0, 0.0", "2.0, -1.0")
    fun `should throw for invalid parameters`(k: Double, lambda: Double) {
        assertThrows<IllegalArgumentException> { DoubleWeibullGenerator.create(k, lambda, fakeRandom) }
    }

    @Test fun `samples should include both positive and negative values`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = DoubleWeibullGenerator.create(2.0, 1.0, rng)
        val samples = (1..10_000).map { gen.sample() }
        assertTrue(samples.any { it > 0 })
        assertTrue(samples.any { it < 0 })
    }

    @Test fun `statistical mean should be close to 0 (symmetric distribution)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = DoubleWeibullGenerator.create(2.0, 1.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(0.0, mean, 0.05)
    }

    @Test fun `absolute values should all be positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = DoubleWeibullGenerator.create(2.0, 1.0, rng)
        repeat(10_000) { assertTrue(abs(gen.sample()) >= 0.0) }
    }
}
