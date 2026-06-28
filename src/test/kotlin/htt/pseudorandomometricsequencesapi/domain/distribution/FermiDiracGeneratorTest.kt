package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.exp

class FakeCommonsRandomForFermiDirac : RandomGenerator {
    var value = 0.3
    override fun nextDouble() = value
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class FermiDiracGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForFermiDirac()

    @Test fun `create should use default value (epsilon over kT = 1 0) when parameter is null`() {
        val gen = FermiDiracGenerator.create(null, fakeRandom)
        assertNotNull(gen)
    }

    @Test fun `sample returns only 0 or 1 (Pauli exclusion principle)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = FermiDiracGenerator.create(1.0, rng)
        repeat(10_000) {
            val n = gen.sample()
            assertTrue(n == 0.0 || n == 1.0)
        }
    }

    @Test fun `with high energy level mode should almost always be empty`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = FermiDiracGenerator.create(10.0, rng) // very high energy → mostly 0
        val mean = (1..10_000).map { gen.sample() }.average()
        assertTrue(mean < 0.01)
    }

    @Test fun `with large negative ekT (degenerate Fermi gas) mode should almost always be occupied`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = FermiDiracGenerator.create(-10.0, rng) // well below Fermi level → mostly 1
        val mean = (1..10_000).map { gen.sample() }.average()
        assertTrue(mean > 0.99)
    }

    @Test fun `statistical P(n=1) should equal 1 div (exp(ekT) + 1)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val ekT = 1.5
        val gen = FermiDiracGenerator.create(ekT, rng)
        val freq1 = (1..50_000).map { gen.sample() }.average()
        val theoretical = 1.0 / (exp(ekT) + 1.0)
        assertEquals(theoretical, freq1, 0.05)
    }
}
 