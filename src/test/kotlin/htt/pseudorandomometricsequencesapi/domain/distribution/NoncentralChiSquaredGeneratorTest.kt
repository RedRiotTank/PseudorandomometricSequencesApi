package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForNoncentralChiSquared : RandomGenerator {
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

class NoncentralChiSquaredGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForNoncentralChiSquared()

    @Test
    fun `create should use default values (df=3 0, lambda=1 0) when parameters are null`() {
        val gen = NoncentralChiSquaredGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() > 0.0)
    }

    @ParameterizedTest(name = "NoncentralChiSquared should fail with df={0}, lambda={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "3.0, -0.1")
    fun `should throw for invalid parameters`(df: Double, lambda: Double) {
        assertThrows<IllegalArgumentException> { NoncentralChiSquaredGenerator.create(df, lambda, fakeRandom) }
    }

    @Test
    fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = NoncentralChiSquaredGenerator.create(3.0, 2.0, rng)
        repeat(5_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test
    fun `statistical mean should be close to df + lambda`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val df = 5.0; val lambda = 3.0
        val gen = NoncentralChiSquaredGenerator.create(df, lambda, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical: df + lambda = 8.0
        assertEquals(8.0, mean, 0.5)
    }

    @Test
    fun `with lambda=0 mean should equal df (reduces to central chi-squared)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val df = 4.0
        val gen = NoncentralChiSquaredGenerator.create(df, 0.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Reduces to central chi-squared, theoretical mean = df = 4.0
        assertEquals(df, mean, 0.2)
    }
}