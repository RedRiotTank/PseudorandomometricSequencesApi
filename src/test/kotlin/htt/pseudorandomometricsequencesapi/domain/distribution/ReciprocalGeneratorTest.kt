package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.ln
import kotlin.math.sqrt

class FakeCommonsRandomForReciprocal : RandomGenerator {
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

class ReciprocalGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForReciprocal()

    @Test
    fun `create should use default values (a=0 1, b=1 0) when parameters are null`() {
        val gen = ReciprocalGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // U=0.5, a=0.1, b=1.0: X = 0.1 * (10)^0.5 = 0.1 * √10
        assertEquals(0.1 * sqrt(10.0), gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "Reciprocal should fail with a={0}, b={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "1.0, 0.5", "1.0, 1.0")
    fun `should throw for invalid bounds`(a: Double, b: Double) {
        assertThrows<IllegalArgumentException> { ReciprocalGenerator.create(a, b, fakeRandom) }
    }

    @Test
    fun `sample formula X = a * (b div a)^U is correct`() {
        // a=1, b=4, U=0.5: X = 1 * (4)^0.5 = 2.0
        val gen = ReciprocalGenerator.create(1.0, 4.0, fakeRandom)
        assertEquals(2.0, gen.sample(), 1e-9)
    }

    @Test
    fun `all samples should be within the bounds a and b`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = ReciprocalGenerator.create(1.0, 100.0, rng)
        repeat(10_000) {
            val x = gen.sample()
            assertTrue(x >= 1.0 && x <= 100.0)
        }
    }

    @Test
    fun `statistical mean should be close to (b-a) divided by ln(b div a)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val a = 1.0; val b = 10.0
        val gen = ReciprocalGenerator.create(a, b, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical: (b-a)/ln(b/a) = 9/ln(10) ≈ 3.908
        val theoretical = (b - a) / ln(b / a)
        assertEquals(theoretical, mean, 0.1)
    }
}
