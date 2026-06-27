package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForCMP : RandomGenerator {
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

class ConwayMaxwellPoissonGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForCMP()

    @Test
    fun `create should use default values (lambda=2 0, nu=1 0) when parameters are null`() {
        val gen = ConwayMaxwellPoissonGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() >= 0.0)
    }

    @ParameterizedTest(name = "CMP should fail with lambda={0}, nu={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "2.0, -0.1")
    fun `should throw for invalid parameters`(lambda: Double, nu: Double) {
        assertThrows<IllegalArgumentException> { ConwayMaxwellPoissonGenerator.create(lambda, nu, fakeRandom) }
    }

    @Test
    fun `output should be a non-negative integer`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = ConwayMaxwellPoissonGenerator.create(3.0, 1.0, rng)
        repeat(1_000) {
            val x = gen.sample()
            assertTrue(x >= 0.0)
            assertTrue(x % 1.0 == 0.0)
        }
    }

    @Test
    fun `with nu=1 it reduces to Poisson and mean should equal lambda`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val lambda = 3.0
        val gen = ConwayMaxwellPoissonGenerator.create(lambda, 1.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // CMP(λ, ν=1) = Poisson(λ): mean = λ = 3.0
        assertEquals(lambda, mean, 0.1)
    }

    @Test
    fun `underdispersed nu greater than 1 should have lower variance than Poisson with same mean`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val poisson = ConwayMaxwellPoissonGenerator.create(3.0, 1.0, rng1)
        val underdispersed = ConwayMaxwellPoissonGenerator.create(3.0, 2.0, rng2)
        fun variance(gen: ConwayMaxwellPoissonGenerator): Double {
            val s = (1..20_000).map { gen.sample() }
            val m = s.average()
            return s.map { (it - m) * (it - m) }.average()
        }
        assertTrue(variance(underdispersed) < variance(poisson))
    }
}
