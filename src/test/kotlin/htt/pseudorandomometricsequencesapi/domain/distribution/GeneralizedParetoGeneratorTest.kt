package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.ln

class FakeCommonsRandomForGeneralizedPareto : RandomGenerator {
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

class GeneralizedParetoGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForGeneralizedPareto()

    @Test
    fun `create should use default values (mu=0, sigma=1, xi=0) when parameters are null`() {
        val gen = GeneralizedParetoGenerator.create(null, null, null, fakeRandom)
        // ξ=0 → Exponential: X = -ln(1-U) = -ln(0.5) = ln(2)
        assertEquals(-ln(0.5), gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "GPD should fail with sigma={1}")
    @CsvSource("0.0, 0.0, 0.0", "0.0, -1.0, 0.0")
    fun `should throw when scale is not positive`(mu: Double, sigma: Double, xi: Double) {
        assertThrows<IllegalArgumentException> { GeneralizedParetoGenerator.create(mu, sigma, xi, fakeRandom) }
    }

    @Test
    fun `sample with xi=0 uses exponential formula X = mu - sigma * ln(1-U)`() {
        val gen = GeneralizedParetoGenerator.create(0.0, 1.0, 0.0, fakeRandom)
        // U=0.5: X = -ln(0.5) ≈ 0.6931
        assertEquals(-ln(0.5), gen.sample(), 1e-9)
    }

    @Test
    fun `sample with xi=1 uses power formula X = sigma div xi * ((1-U)^{-xi} - 1)`() {
        val gen = GeneralizedParetoGenerator.create(0.0, 1.0, 1.0, fakeRandom)
        // U=0.5, xi=1: X = 1*(0.5^(-1)-1)/1 = 2-1 = 1.0
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @Test
    fun `statistical mean with xi=0 (Exponential) should be close to sigma=1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = GeneralizedParetoGenerator.create(0.0, 1.0, 0.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Exponential(sigma=1.0) mean = 1.0
        assertEquals(1.0, mean, 0.1)
    }
}