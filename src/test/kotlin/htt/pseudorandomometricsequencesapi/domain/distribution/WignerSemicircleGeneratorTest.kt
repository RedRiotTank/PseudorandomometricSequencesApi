package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs

class FakeCommonsRandomForWignerSemicircle : RandomGenerator {
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

class WignerSemicircleGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForWignerSemicircle()

    @Test
    fun `create should use default value (R=1 0) when parameter is null`() {
        val generator = WignerSemicircleGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        // With nextDouble()=0.5 always: x = 0.5*2*1-1 = 0, y = 0, x^2+y^2=0 <= R^2=1, so returns 0
        val sample = generator.sample()
        assertTrue(sample >= -1.0 && sample <= 1.0) { "Sample must be in [-R, R]" }
    }

    @ParameterizedTest(name = "WignerSemicircleGenerator should fail with R: {0}")
    @ValueSource(doubles = [0.0, -1.0, -5.5])
    fun `should throw exception if R is not positive`(r: Double) {
        assertThrows<IllegalArgumentException> {
            WignerSemicircleGenerator.create(r, fakeRandom)
        }
    }

    @Test
    fun `all samples should be within radius bounds`() {
        val R = 2.0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = WignerSemicircleGenerator.create(R, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it >= -R && it <= R }) {
            "All Wigner Semicircle samples must be in [-$R, $R]"
        }
    }

    @Test
    fun `sample mean should be close to 0 (symmetric distribution)`() {
        // Wigner Semicircle is symmetric, so mean = 0
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = WignerSemicircleGenerator.create(1.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(abs(sampleMean) < 0.05) {
            "Expected sample mean close to 0.0, but was $sampleMean"
        }
    }
}
      