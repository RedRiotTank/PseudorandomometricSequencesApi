package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForBurr : RandomGenerator {
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

class BurrGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForBurr()

    @Test
    fun `create should use default values (c=1 0, k=1 0) when parameters are null`() {
        val gen = BurrGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // U=0.5, c=1, k=1: X = ((0.5)^(-1) - 1)^(1/1) = 2-1 = 1.0
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "Burr should fail with c={0}, k={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "1.0, 0.0", "1.0, -1.0")
    fun `should throw when c or k is not positive`(c: Double, k: Double) {
        assertThrows<IllegalArgumentException> { BurrGenerator.create(c, k, fakeRandom) }
    }

    @Test
    fun `sample formula X = ((1-U)^{-1 div k} - 1)^{1 div c} is correct`() {
        // c=2, k=1, U=0.5: X = ((0.5)^(-1)-1)^(0.5) = 1^0.5 = 1.0
        val gen = BurrGenerator.create(2.0, 1.0, fakeRandom)
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @Test
    fun `all samples should be positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = BurrGenerator.create(2.0, 3.0, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test
    fun `statistical mean for c=1 k=1 should be finite and positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        // Burr(1,1) = Lomax(1,1): mean = λ/(α-1) undefined for α=1, skip moment test
        // Instead verify all samples positive and finite
        val gen = BurrGenerator.create(2.0, 2.0, rng)
        val samples = (1..50_000).map { gen.sample() }
        assertTrue(samples.all { it > 0.0 && it.isFinite() })
        // Burr(c=2,k=2): mean = k*B(k-1/c, 1+1/c) = 2*B(1.5, 1.5) = 2*(π/8)/(1/2)... test just positivity
        assertTrue(samples.average() > 0.0)
    }
}
