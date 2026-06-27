package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FakeCommonsRandomForYuleSimon : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class YuleSimonGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForYuleSimon()

    @Test fun `create should use default value (rho=1 5) when parameter is null`() {
        val gen = YuleSimonGenerator.create(null, fakeRandom)
        assertNotNull(gen)
        assertTrue(gen.sample() >= 1.0)
    }

    @ParameterizedTest(name = "YuleSimon should fail with rho={0}")
    @ValueSource(doubles = [0.0, -1.0])
    fun `should throw when rho is not positive`(rho: Double) {
        assertThrows<IllegalArgumentException> { YuleSimonGenerator.create(rho, fakeRandom) }
    }

    @Test fun `all samples should be positive integers starting at 1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = YuleSimonGenerator.create(2.0, rng)
        repeat(5_000) {
            val x = gen.sample()
            assertTrue(x >= 1.0)
            assertTrue(x % 1.0 == 0.0)
        }
    }

    @Test fun `statistical mean should be close to rho divided by (rho - 1) for rho greater than 1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val rho = 2.0
        val gen = YuleSimonGenerator.create(rho, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean = rho / (rho - 1) = 2.0
        assertEquals(2.0, mean, 0.2)
    }

    @Test fun `small values should dominate (power-law behaviour)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = YuleSimonGenerator.create(1.5, rng)
        val samples = (1..10_000).map { gen.sample() }
        // P(X=1) = rho/(rho+1) = 1.5/2.5 = 0.6
        val freq1 = samples.count { it == 1.0 }.toDouble() / samples.size
        assertEquals(0.6, freq1, 0.05)
    }
}