package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.exp

class FakeCommonsRandomForBoseEinstein : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class BoseEinsteinGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForBoseEinstein()

    @Test fun `create should use default value (epsilon over kT = 1 0) when parameter is null`() {
        val gen = BoseEinsteinGenerator.create(null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "BoseEinstein should fail with epsilon over kT = {0}")
    @ValueSource(doubles = [0.0, -1.0])
    fun `should throw when reduced energy is not positive`(ekT: Double) {
        assertThrows<IllegalArgumentException> { BoseEinsteinGenerator.create(ekT, fakeRandom) }
    }

    @Test fun `output should be non-negative integers`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = BoseEinsteinGenerator.create(1.0, rng)
        repeat(5_000) {
            val n = gen.sample()
            assertTrue(n >= 0.0)
            assertTrue(n % 1.0 == 0.0)
        }
    }

    @Test fun `statistical mean should be close to 1 div (exp(epsilon over kT) - 1)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val ekT = 0.5
        val gen = BoseEinsteinGenerator.create(ekT, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        val theoretical = 1.0 / (exp(ekT) - 1.0)
        assertEquals(theoretical, mean, theoretical * 0.1)
    }

    @Test fun `lower energy should give higher mean occupation (more bosons per mode)`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val low  = BoseEinsteinGenerator.create(0.1, rng1)
        val high = BoseEinsteinGenerator.create(2.0, rng2)
        val m1 = (1..20_000).map { low.sample()  }.average()
        val m2 = (1..20_000).map { high.sample() }.average()
        assertTrue(m1 > m2)
    }
}
