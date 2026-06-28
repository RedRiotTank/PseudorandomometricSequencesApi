package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FakeCommonsRandomForPlanck : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class PlanckGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForPlanck()

    @Test fun `create should use default value (T=1 0) when parameter is null`() {
        val gen = PlanckGenerator.create(null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() > 0.0)
    }

    @ParameterizedTest(name = "Planck should fail with T={0}")
    @ValueSource(doubles = [0.0, -1.0])
    fun `should throw when temperature is not positive`(T: Double) {
        assertThrows<IllegalArgumentException> { PlanckGenerator.create(T, fakeRandom) }
    }

    @Test fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = PlanckGenerator.create(1.0, rng)
        repeat(5_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `statistical mean should be close to 2 701 * T (Planck mean)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val T = 1.0
        val gen = PlanckGenerator.create(T, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean = 3*zeta(4)/zeta(3) * T ≈ 2.701 * T
        assertEquals(2.701 * T, mean, 0.3)
    }

    @Test fun `higher temperature should produce proportionally higher mean energy`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val cool = PlanckGenerator.create(1.0, rng1)
        val hot  = PlanckGenerator.create(3.0, rng2)
        val mean1 = (1..20_000).map { cool.sample() }.average()
        val mean2 = (1..20_000).map { hot.sample()  }.average()
        assertTrue(mean2 > mean1) { "Higher temperature should yield higher mean energy" }
    }
}