package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.PI
import kotlin.math.tan

class FakeCommonsRandomForHalfCauchy : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class HalfCauchyGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForHalfCauchy()

    @Test fun `create should use default value (gamma=1 0) when parameter is null`() {
        val gen = HalfCauchyGenerator.create(null, fakeRandom)
        assertNotNull(gen)
        // U=0.5: X = 1*tan(π/4) = 1.0
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "HalfCauchy should fail with gamma={0}")
    @ValueSource(doubles = [0.0, -1.0])
    fun `should throw when scale is not positive`(gamma: Double) {
        assertThrows<IllegalArgumentException> { HalfCauchyGenerator.create(gamma, fakeRandom) }
    }

    @Test fun `sample formula X = gamma * tan(pi*U div 2) is correct`() {
        val gamma = 2.0
        val gen = HalfCauchyGenerator.create(gamma, fakeRandom)
        // U=0.5: X = 2*tan(π/4) = 2.0
        assertEquals(2.0, gen.sample(), 1e-9)
    }

    @Test fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = HalfCauchyGenerator.create(1.0, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `median should be close to gamma (since F(gamma)=0 5)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gamma = 2.0
        val gen = HalfCauchyGenerator.create(gamma, rng)
        val samples = (1..50_000).map { gen.sample() }.sorted()
        val median = samples[25_000]
        // Theoretical median = gamma
        assertEquals(gamma, median, 0.1)
    }
}
  