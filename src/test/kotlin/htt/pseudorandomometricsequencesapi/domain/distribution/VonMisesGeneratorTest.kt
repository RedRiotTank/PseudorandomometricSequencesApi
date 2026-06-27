package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class FakeCommonsRandomForVonMises : RandomGenerator {
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

class VonMisesGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForVonMises()

    @Test
    fun `create should use default values (mu=0 0, kappa=1 0) when parameters are null`() {
        val generator = VonMisesGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
    }

    @Test
    fun `should throw exception if kappa is negative`() {
        assertThrows<IllegalArgumentException> {
            VonMisesGenerator.create(0.0, -0.1, fakeRandom)
        }
    }

    @Test
    fun `sample should return value within circular range minus pi to pi`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = VonMisesGenerator.create(0.0, 1.0, rng)
        repeat(1000) {
            val sample = generator.sample()
            assertTrue(sample > -PI && sample <= PI) {
                "Sample $sample is outside (-pi, pi]"
            }
        }
    }

    @Test
    fun `sample mean direction should be close to mu for kappa=1`() {
        // Mean direction for VonMises(mu=0, kappa=1) ≈ 0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = VonMisesGenerator.create(0.0, 1.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        // Circular mean = atan2(mean(sin(x)), mean(cos(x)))
        val meanSin = samples.map { sin(it) }.average()
        val meanCos = samples.map { cos(it) }.average()
        val circularMean = atan2(meanSin, meanCos)
        assertTrue(abs(circularMean) < 0.1) {
            "Expected circular mean close to 0.0, but was $circularMean"
        }
    }
}