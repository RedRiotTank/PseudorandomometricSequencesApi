
package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForTsallis : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class TsallisGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForTsallis()

    @Test fun `create should use default values (T=0 1, n=7 0) when parameters are null`() {
        val gen = TsallisGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() >= 0.0)
    }

    @ParameterizedTest(name = "Tsallis should fail with T={0}, n={1}")
    @CsvSource("0.0, 7.0", "-1.0, 7.0", "0.1, 1.0", "0.1, 0.5")
    fun `should throw for invalid parameters`(T: Double, n: Double) {
        assertThrows<IllegalArgumentException> { TsallisGenerator.create(T, n, fakeRandom) }
    }

    @Test fun `formula with U=0 5 should give correct value`() {
        // (n-1)*T*((1-0.5)^(-1/(n-1)) - 1) = 6*0.1*(2^(1/6)-1) for T=0.1, n=7
        val gen = TsallisGenerator.create(0.1, 7.0, fakeRandom)
        val expected = 6.0 * 0.1 * (Math.pow(0.5, -1.0 / 6.0) - 1.0)
        assertEquals(expected, gen.sample(), 1e-9)
    }

    @Test fun `all samples should be non-negative`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = TsallisGenerator.create(0.1, 7.0, rng)
        repeat(10_000) { assertTrue(gen.sample() >= 0.0) }
    }

    @Test fun `statistical mean should be close to (n-1)*T div (n-2) for n greater than 2`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val T = 0.2; val n = 5.0
        val gen = TsallisGenerator.create(T, n, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // mean = (n-1)*T/(n-2) = 4*0.2/3 ≈ 0.2667
        assertEquals((n - 1) * T / (n - 2), mean, 0.05)
    }
}
