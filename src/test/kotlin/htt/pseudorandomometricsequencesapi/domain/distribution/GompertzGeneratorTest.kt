package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.ln

class FakeCommonsRandomForGompertz : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class GompertzGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForGompertz()

    @Test fun `create should use default values (eta=1 0, b=1 0) when parameters are null`() {
        val gen = GompertzGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // U=0.5, η=1, b=1: X = ln(1 - 1*ln(0.5)) = ln(1 + ln2)
        assertEquals(ln(1.0 - ln(0.5)), gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "Gompertz should fail with eta={0}, b={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "1.0, 0.0", "1.0, -1.0")
    fun `should throw for invalid parameters`(eta: Double, b: Double) {
        assertThrows<IllegalArgumentException> { GompertzGenerator.create(eta, b, fakeRandom) }
    }

    @Test fun `sample formula X = (1 div b) * ln(1 - (b div eta) * ln(U)) is correct`() {
        val gen = GompertzGenerator.create(1.0, 1.0, fakeRandom)
        val expected = ln(1.0 - ln(0.5))
        assertEquals(expected, gen.sample(), 1e-9)
    }

    @Test fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = GompertzGenerator.create(1.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `statistical samples should be positive and finite`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = GompertzGenerator.create(1.0, 1.0, rng)
        val samples = (1..20_000).map { gen.sample() }
        assertTrue(samples.all { it > 0.0 && it.isFinite() })
    }
}
