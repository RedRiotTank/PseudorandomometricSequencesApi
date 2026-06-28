package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForVoigt : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0  // Gaussian returns 0, Cauchy returns 0 at U=0.5
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class VoigtGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForVoigt()

    @Test fun `create should use default values (mu=0 0, sigma=1 0, gamma=0 5) when parameters are null`() {
        val gen = VoigtGenerator.create(null, null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "Voigt should fail with sigma={1}, gamma={2}")
    @CsvSource("0.0, 0.0, 0.5", "0.0, -1.0, 0.5", "0.0, 1.0, 0.0", "0.0, 1.0, -1.0")
    fun `should throw for invalid scale parameters`(mu: Double, sigma: Double, gamma: Double) {
        assertThrows<IllegalArgumentException> { VoigtGenerator.create(mu, sigma, gamma, fakeRandom) }
    }

    @Test fun `all samples should be finite`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = VoigtGenerator.create(0.0, 1.0, 0.5, rng)
        repeat(10_000) { assertTrue(gen.sample().isFinite()) }
    }

    @Test fun `statistical median should be close to location mu (Voigt mean is undefined)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu = 3.0
        val gen = VoigtGenerator.create(mu, 1.0, 0.5, rng)
        val sorted = (1..50_000).map { gen.sample() }.sorted()
        val median = sorted[25_000]
        assertEquals(mu, median, 0.2)
    }

    @Test fun `variance should exceed pure Gaussian variance (Cauchy adds heavy tails)`() {
        val rng1 = JDKRandomGenerator().also { it.setSeed(42L) }
        val rng2 = JDKRandomGenerator().also { it.setSeed(42L) }
        val voigt = VoigtGenerator.create(0.0, 1.0, 0.5, rng1)
        // Pure Gaussian: sigma=1
        val gaussian = GaussianGenerator.create(0.0, 1.0, java.util.Random(42L))
        val s1 = (1..20_000).map { voigt.sample() }; val m1 = s1.average()
        val v1 = s1.filter { it.isFinite() }.map { (it - m1) * (it - m1) }.average()
        val s2 = (1..20_000).map { gaussian.sample() }; val m2 = s2.average()
        val v2 = s2.map { (it - m2) * (it - m2) }.average()
        // Voigt should have higher variance
        assertTrue(v1 > v2) { "Voigt distribution should have higher variance due to heavy tails" }
    }
}