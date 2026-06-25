package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FakeCommonsRandomForGeometric : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 1
    override fun nextInt(n: Int) = 1
    override fun nextLong() = 1L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = true
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
}

class GeometricGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForGeometric()

    @Test
    fun `create should use default value (p=0 5) when parameter is null`() {
        val generator = GeometricGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        assertTrue(generator.distribution.probabilityOfSuccess == 0.5)
    }

    @ParameterizedTest(name = "GeometricGenerator should fail with p: {0}")
    @ValueSource(doubles = [0.0, -0.1, 1.1, -5.0])
    fun `should throw exception if p is not in the valid range exclusive 0 to 1 inclusive`(p: Double) {
        assertThrows<IllegalArgumentException> {
            GeometricGenerator.create(p, fakeRandom)
        }
    }

    @Test
    fun `sample should return a non-negative integer as Double`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = GeometricGenerator.create(0.5, rng)
        val N = 1_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= 0.0 && it % 1.0 == 0.0 }) {
            "All Geometric samples must be non-negative integers"
        }
    }

    @Test
    fun `sample mean should be close to theoretical mean for p=0 5`() {
        // Geometric(p=0.5) models # failures before first success (0-based), mean = (1-p)/p = 1.0
        val theoreticalMean = 1.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = GeometricGenerator.create(0.5, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(sampleMean in theoreticalMean * 0.95..theoreticalMean * 1.05) {
            "Expected mean near $theoreticalMean but got $sampleMean"
        }
    }
}
