package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForNegHyper : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class NegativeHypergeometricGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForNegHyper()

    @Test fun `create should use default values (N=50, K=25, r=5) when parameters are null`() {
        val gen = NegativeHypergeometricGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() >= 0.0)
    }

    @ParameterizedTest(name = "NegHyper should fail with N={0}, K={1}, r={2}")
    @CsvSource(
        "0.0, 25.0, 5.0",   // N not positive
        "1.5, 25.0, 5.0",   // N not integer
        "50.0, -1.0, 5.0",  // K negative
        "50.0, 60.0, 5.0",  // K > N
        "50.0, 25.0, 0.0",  // r not positive
        "50.0, 25.0, 30.0"  // r > K
    )
    fun `should throw for invalid parameters`(n: Double, k: Double, r: Double) {
        assertThrows<IllegalArgumentException> { NegativeHypergeometricGenerator.create(n, k, r, fakeRandom) }
    }

    @Test fun `output should be a non-negative integer`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = NegativeHypergeometricGenerator.create(50.0, 25.0, 5.0, rng)
        repeat(2_000) {
            val x = gen.sample()
            assertTrue(x >= 0.0)
            assertTrue(x % 1.0 == 0.0)
        }
    }

    @Test fun `statistical mean should be close to r*(N-K) divided by (K+1)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        // N=50, K=25, r=5: mean = 5*25/26 ≈ 4.808
        val gen = NegativeHypergeometricGenerator.create(50.0, 25.0, 5.0, rng)
        val mean = (1..20_000).map { gen.sample() }.average()
        val theoretical = 5.0 * 25.0 / 26.0
        assertEquals(theoretical, mean, theoretical * 0.1)
    }

    @Test fun `with r=K all successes must be drawn and failures count cannot exceed N-K`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        // N=20, K=5, r=5 (draw all successes): failures ≤ N-K = 15
        val gen = NegativeHypergeometricGenerator.create(20.0, 5.0, 5.0, rng)
        repeat(1_000) {
            val failures = gen.sample()
            assertTrue(failures <= 15.0) { "Failures cannot exceed N-K (15)" }
        }
    }
}
    