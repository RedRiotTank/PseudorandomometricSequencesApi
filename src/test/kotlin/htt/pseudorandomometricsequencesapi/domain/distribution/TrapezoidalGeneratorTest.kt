package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForTrapezoidal : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class TrapezoidalGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForTrapezoidal()

    @Test fun `create should use default values (a=0 0, d=1 0, frac=0 5) when parameters are null`() {
        val gen = TrapezoidalGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
        // Default is symmetric trapezoid [0,1] plateau_frac=0.5 → b=0.25, c=0.75
        // With U=0.5, which is in the plateau region: X = b + (U - u1)/h
        val x = gen.sample()
        assertTrue(x >= 0.0 && x <= 1.0)
    }

    @ParameterizedTest(name = "Trapezoidal should fail with a={0}, d={1}, frac={2}")
    @CsvSource("1.0, 0.5, 0.5", "1.0, 1.0, 0.5", "0.0, 1.0, 0.0", "0.0, 1.0, 1.1")
    fun `should throw for invalid parameters`(a: Double, d: Double, frac: Double) {
        assertThrows<IllegalArgumentException> { TrapezoidalGenerator.create(a, d, frac, fakeRandom) }
    }

    @Test fun `with plateau fraction=1 it reduces to Uniform and X = a + U*(d-a)`() {
        // frac=1: b=a, c=d → uniform. U=0.5, a=0, d=1 → X=0.5
        val gen = TrapezoidalGenerator.create(0.0, 1.0, 1.0, fakeRandom)
        assertEquals(0.5, gen.sample(), 1e-9)
    }

    @Test fun `all samples should be within the bounds a and d`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = TrapezoidalGenerator.create(2.0, 8.0, 0.5, rng)
        repeat(10_000) { val x = gen.sample(); assertTrue(x >= 2.0 && x <= 8.0) }
    }

    @Test fun `statistical mean should be close to (a+d) divided by 2 for symmetric trapezoid`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = TrapezoidalGenerator.create(0.0, 1.0, 0.5, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(0.5, mean, 0.01)
    }
}