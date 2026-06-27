package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForGenGamma : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class GeneralizedGammaGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForGenGamma()

    @Test fun `create should use default values (a=1 0, d=1 0, p=1 0) when parameters are null`() {
        val gen = GeneralizedGammaGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() > 0.0)
    }

    @ParameterizedTest(name = "GenGamma should fail with a={0}, d={1}, p={2}")
    @CsvSource("0.0, 1.0, 1.0", "-1.0, 1.0, 1.0", "1.0, 0.0, 1.0", "1.0, 1.0, 0.0")
    fun `should throw for invalid parameters`(a: Double, d: Double, p: Double) {
        assertThrows<IllegalArgumentException> { GeneralizedGammaGenerator.create(a, d, p, fakeRandom) }
    }

    @Test fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = GeneralizedGammaGenerator.create(1.0, 1.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `with d=1 p=1 it reduces to Gamma(1 1) Exponential and mean should be close to a`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val a = 2.0
        val gen = GeneralizedGammaGenerator.create(a, 1.0, 1.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Gamma(1/1, 1) = Exp(1), scaled by a: mean = a * 1 = 2.0
        assertEquals(a, mean, a * 0.05)
    }

    @Test fun `with d=p=2 it behaves like Rayleigh and mean should be positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = GeneralizedGammaGenerator.create(1.0, 2.0, 2.0, rng)
        val samples = (1..20_000).map { gen.sample() }
        assertTrue(samples.all { it > 0.0 })
    }
}
