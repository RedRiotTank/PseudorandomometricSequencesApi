package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.ln

/**
 * For TukeyLambda with lambda=0: X = ln(U/(1-U))
 * With U=0.5: X = ln(0.5/0.5) = ln(1) = 0.0
 * With U=0.75: X = ln(0.75/0.25) = ln(3) ≈ 1.0986
 */
class FakeCommonsRandomForTukeyLambdaHalf : RandomGenerator {
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

class FakeCommonsRandomForTukeyLambdaThreeQuarters : RandomGenerator {
    override fun nextDouble() = 0.75
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

class TukeyLambdaGeneratorTest {

    @Test
    fun `create should use default value (lambda=0 0) when parameter is null`() {
        val fakeRandom = FakeCommonsRandomForTukeyLambdaHalf()
        val generator = TukeyLambdaGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        // lambda=0, U=0.5: X = ln(0.5/0.5) = ln(1) = 0.0
        assertEquals(0.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample should calculate X = ln(U over (1-U)) for lambda=0 and U=0 5`() {
        val fakeRandom = FakeCommonsRandomForTukeyLambdaHalf()
        val generator = TukeyLambdaGenerator.create(0.0, fakeRandom)
        // U=0.5: X = ln(1) = 0.0
        assertEquals(0.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample should calculate X = ln(U over (1-U)) for lambda=0 and U=0 75`() {
        val fakeRandom = FakeCommonsRandomForTukeyLambdaThreeQuarters()
        val generator = TukeyLambdaGenerator.create(0.0, fakeRandom)
        // U=0.75: X = ln(0.75/0.25) = ln(3) ≈ 1.0986
        val expected = ln(3.0)
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @Test
    fun `sample should calculate X = (U to lambda minus (1-U) to lambda) over lambda for lambda=1`() {
        val fakeRandom = FakeCommonsRandomForTukeyLambdaHalf()
        val generator = TukeyLambdaGenerator.create(1.0, fakeRandom)
        // U=0.5, lambda=1: X = (0.5^1 - 0.5^1)/1 = 0.0
        assertEquals(0.0, generator.sample(), 1e-9)
    }

    @Test
    fun `sample mean should be close to 0 (symmetric distribution)`() {
        // TukeyLambda is symmetric so mean = 0 for any lambda
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = TukeyLambdaGenerator.create(0.0, rng)
        val N = 50_000
        val sampleMean = (1..N).map { generator.sample() }.average()
        assertTrue(abs(sampleMean) < 0.1) {
            "Expected mean near 0 but got $sampleMean"
        }
    }
}
