package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForScaledInverseChiSquared : RandomGenerator {
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

class ScaledInverseChiSquaredGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForScaledInverseChiSquared()

    @Test
    fun `create should use default values (nu=3 0, tau2=1 0) when parameters are null`() {
        val gen = ScaledInverseChiSquaredGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() > 0.0)
    }

    @ParameterizedTest(name = "ScaledInvChiSq should fail with nu={0}, tau2={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "3.0, 0.0", "3.0, -1.0")
    fun `should throw for invalid parameters`(nu: Double, tau2: Double) {
        assertThrows<IllegalArgumentException> { ScaledInverseChiSquaredGenerator.create(nu, tau2, fakeRandom) }
    }

    @Test
    fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = ScaledInverseChiSquaredGenerator.create(4.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test
    fun `statistical mean should be close to nu * tau2 divided by (nu - 2) for nu greater than 2`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val nu = 6.0; val tau2 = 1.0
        val gen = ScaledInverseChiSquaredGenerator.create(nu, tau2, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical: nu*tau2/(nu-2) = 6/4 = 1.5
        assertEquals(1.5, mean, 0.1)
    }
}