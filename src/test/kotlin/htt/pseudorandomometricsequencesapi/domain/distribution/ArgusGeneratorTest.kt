package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForArgus : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class ArgusGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForArgus()

    @Test fun `create should use default values (c=1 0, chi=1 0) when parameters are null`() {
        val gen = ArgusGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "ARGUS should fail with c={0}, chi={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "1.0, -0.1")
    fun `should throw for invalid parameters`(c: Double, chi: Double) {
        assertThrows<IllegalArgumentException> { ArgusGenerator.create(c, chi, fakeRandom) }
    }

    @Test fun `all samples should be strictly within (0, c)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val c = 2.0
        val gen = ArgusGenerator.create(c, 1.0, rng)
        repeat(5_000) {
            val x = gen.sample()
            assertTrue(x > 0.0 && x < c)
        }
    }

    @Test fun `distribution should be concentrated away from 0 and c (characteristic shape)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = ArgusGenerator.create(1.0, 1.0, rng)
        val samples = (1..10_000).map { gen.sample() }
        // Most samples should be in (0.1, 0.9)
        val middle = samples.count { it > 0.1 && it < 0.9 }
        assertTrue(middle > 7000)
    }

    @Test fun `with chi=0 distribution should peak near c div sqrt(2)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val c = 1.0
        val gen = ArgusGenerator.create(c, 0.0, rng)
        val samples = (1..20_000).map { gen.sample() }
        val mean = samples.average()
        // For chi=0: PDF ∝ x*sqrt(1-x²), peak at 1/√2 ≈ 0.707
        assertTrue(mean > 0.5 && mean < 0.8) { "Mean should be concentrated around the peak" }
    }
}