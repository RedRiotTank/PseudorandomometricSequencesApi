package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForRBW : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class RelativisticBreitWignerGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForRBW()

    @Test fun `create should use default values (m0=1 0, gamma=0 1) when parameters are null`() {
        val gen = RelativisticBreitWignerGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() > 0.0)
    }

    @ParameterizedTest(name = "RelBW should fail with m0={0}, gamma={1}")
    @CsvSource("0.0, 0.1", "-1.0, 0.1", "1.0, 0.0", "1.0, -0.1")
    fun `should throw for invalid parameters`(m0: Double, gamma: Double) {
        assertThrows<IllegalArgumentException> { RelativisticBreitWignerGenerator.create(m0, gamma, fakeRandom) }
    }

    @Test fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = RelativisticBreitWignerGenerator.create(1.0, 0.1, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `statistical median should be close to pole mass m0`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val m0 = 2.0; val gamma = 0.1
        val gen = RelativisticBreitWignerGenerator.create(m0, gamma, rng)
        val sorted = (1..50_000).map { gen.sample() }.sorted()
        val median = sorted[25_000]
        assertEquals(m0, median, m0 * 0.05)
    }

    @Test fun `narrower width should produce more concentrated samples`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val narrow = RelativisticBreitWignerGenerator.create(1.0, 0.01, rng1)
        val wide   = RelativisticBreitWignerGenerator.create(1.0, 0.5,  rng2)
        fun stddev(gen: RelativisticBreitWignerGenerator): Double {
            val s = (1..10_000).map { gen.sample() }.filter { it.isFinite() }
            val m = s.average(); return s.map { (it-m)*(it-m) }.average()
        }
        assertTrue(stddev(wide) > stddev(narrow)) { "Larger gamma should produce higher variance" }
    }
}
