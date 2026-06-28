package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FakeCommonsRandomForMJuttner : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class MaxwellJuttnerGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForMJuttner()

    @Test fun `create should use default value (theta=1 0) when parameter is null`() {
        val gen = MaxwellJuttnerGenerator.create(null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "MaxwellJuttner should fail with theta={0}")
    @ValueSource(doubles = [0.0, -1.0])
    fun `should throw when theta is not positive`(theta: Double) {
        assertThrows<IllegalArgumentException> { MaxwellJuttnerGenerator.create(theta, fakeRandom) }
    }

    @Test fun `all samples should be strictly positive (momentum magnitudes)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = MaxwellJuttnerGenerator.create(1.0, rng)
        repeat(5_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `higher temperature should produce higher mean momentum`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val cool = MaxwellJuttnerGenerator.create(0.1, rng1)
        val hot  = MaxwellJuttnerGenerator.create(5.0, rng2)
        val mean1 = (1..5_000).map { cool.sample() }.average()
        val mean2 = (1..5_000).map { hot.sample()  }.average()
        assertTrue(mean2 > mean1) { "Higher temperature should yield higher mean momentum" }
    }
}