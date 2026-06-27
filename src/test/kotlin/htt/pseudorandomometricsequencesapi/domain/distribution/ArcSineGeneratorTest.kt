package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.PI
import kotlin.math.sin

class FakeCommonsRandomForArcSine : RandomGenerator {
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

class ArcSineGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForArcSine()

    @Test
    fun `create should use default values (a=0 0, b=1 0) when parameters are null`() {
        val gen = ArcSineGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // U=0.5: X = sin²(π*0.5/2) = sin²(π/4) = 0.5
        assertEquals(0.5, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "ArcSine should fail with a={0}, b={1}")
    @CsvSource("1.0, 0.5", "1.0, 1.0")
    fun `should throw when lower bound is not less than upper bound`(a: Double, b: Double) {
        assertThrows<IllegalArgumentException> { ArcSineGenerator.create(a, b, fakeRandom) }
    }

    @Test
    fun `sample formula X = a + (b-a) * sin^2(pi*U 2) is correct`() {
        // U=0.5, a=0, b=1: X = sin²(π/4) = (√2/2)² = 0.5
        val gen = ArcSineGenerator.create(0.0, 1.0, fakeRandom)
        val s = sin(PI * 0.5 / 2.0)
        assertEquals(s * s, gen.sample(), 1e-9)
    }

    @Test
    fun `sample should always be within the bounds a and b`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = ArcSineGenerator.create(2.0, 5.0, rng)
        repeat(10_000) {
            val x = gen.sample()
            assertTrue(x >= 2.0 && x <= 5.0)
        }
    }

    @Test
    fun `statistical mean should be close to (a+b) divided by 2`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = ArcSineGenerator.create(0.0, 1.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // theoretical mean = (a + b) / 2.0 = 0.5
        assertEquals(0.5, mean, 0.05)
    }
}