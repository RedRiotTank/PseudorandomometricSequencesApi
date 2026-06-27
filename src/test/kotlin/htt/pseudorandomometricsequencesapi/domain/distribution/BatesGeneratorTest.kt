package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForBates : RandomGenerator {
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

class BatesGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBates()

    @Test
    fun `create should use default n=12 when parameter is null`() {
        val gen = BatesGenerator.create(null, fakeRandom)
        assertNotNull(gen)
        // n=12, each U=0.5 → mean = 12*0.5/12 = 0.5
        assertEquals(0.5, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "Bates should fail with n={0}")
    @CsvSource("0.0", "-1.0", "1.5", "2.7")
    fun `should throw when n is not a positive integer`(n: Double) {
        assertThrows<IllegalArgumentException> { BatesGenerator.create(n, fakeRandom) }
    }

    @Test
    fun `sample is mean of n uniforms - formula X = sum(Ui) divided by n`() {
        val gen = BatesGenerator.create(4.0, fakeRandom)
        // 4 * 0.5 / 4 = 0.5
        assertEquals(0.5, gen.sample(), 1e-9)
    }

    @Test
    fun `sample should always be in (0, 1)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = BatesGenerator.create(10.0, rng)
        repeat(10_000) {
            val x = gen.sample()
            assertTrue(x >= 0.0 && x <= 1.0)
        }
    }

    @Test
    fun `statistical mean should be close to 0 5 for any n`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = BatesGenerator.create(12.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(0.5, mean, 0.02)
    }

    @Test
    fun `variance should be close to 1 divided by (12 * n) for n=12`() {
        val rng = JDKRandomGenerator().also { it.setSeed(99L) }
        val gen = BatesGenerator.create(12.0, rng)
        val samples = (1..50_000).map { gen.sample() }
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()
        // Theoretical: 1/(12*12) = 1.0 / 144.0
        assertEquals(1.0 / 144.0, variance, 0.001)
    }
}