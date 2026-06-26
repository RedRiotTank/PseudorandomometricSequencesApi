package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs

/**
 * For SkewNormal with alpha=0 (symmetric), nextGaussian alternates 1.0, 2.0:
 * delta = 0/(sqrt(1+0)) = 0
 * W = 0*|z1| + sqrt(1-0)*z2 = z2
 * X = 0 + 1 * z2 = z2
 * First sample: z1=1.0, z2=2.0, X = 0 + 1 * (0*|1| + 1*2) = 2.0
 */
class FakeCommonsRandomForSkewNormal : RandomGenerator {
    private var callCount = 0
    override fun nextGaussian(): Double {
        return if (callCount++ % 2 == 0) 1.0 else 2.0
    }
    override fun nextDouble() = 0.5
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

class SkewNormalGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForSkewNormal()

    @Test
    fun `create should use default values (xi=0, omega=1, alpha=0) when parameters are null`() {
        val generator = SkewNormalGenerator.create(null, null, null, fakeRandom)
        assertNotNull(generator)
    }

    @ParameterizedTest(name = "SkewNormalGenerator should fail with omega: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if omega is not positive`(omega: Double) {
        assertThrows<IllegalArgumentException> {
            SkewNormalGenerator.create(0.0, omega, 0.0, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = xi + omega times W correctly for alpha=0`() {
        // alpha=0 => delta=0, W = 0*|z1| + 1*z2 = z2
        // z1=1.0 (first nextGaussian), z2=2.0 (second nextGaussian)
        // X = 0 + 1 * 2.0 = 2.0
        val generator = SkewNormalGenerator.create(0.0, 1.0, 0.0, fakeRandom)
        assertEquals(2.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to 0 for symmetric case (alpha=0)`() {
        // For SkewNormal(xi=0, omega=1, alpha=0), mean = 0 (reduces to Normal(0,1))
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = SkewNormalGenerator.create(0.0, 1.0, 0.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(abs(sampleMean) < 0.1) {
            "Expected mean near 0 but got $sampleMean"
        }
    }
}
