package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForHyperbolicSecant : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0
    override fun nextInt(n: Int) = 0
    override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
}

class HyperbolicSecantGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForHyperbolicSecant()

    @Test
    fun `create should use default values (mu=0 0, sigma=1 0) when parameters are null`() {
        val gen = HyperbolicSecantGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // U=0.5: tan(π/4)=1, ln(1)=0, X=0.0
        assertEquals(0.0, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "HyperbolicSecant should fail with sigma={1}")
    @CsvSource("0.0, 0.0", "0.0, -1.0")
    fun `should throw when scale is not positive`(mu: Double, sigma: Double) {
        assertThrows<IllegalArgumentException> { HyperbolicSecantGenerator.create(mu, sigma, fakeRandom) }
    }

    @Test
    fun `sample formula X = mu + sigma*(2 div pi)*ln(tan(pi*U div 2)) is correct`() {
        // U=0.5: X = 0 + 1*(2/π)*ln(tan(π/4)) = (2/π)*ln(1) = 0.0
        val gen = HyperbolicSecantGenerator.create(0.0, 1.0, fakeRandom)
        assertEquals(0.0, gen.sample(), 1e-9)
    }

    @Test
    fun `statistical mean should be close to location parameter`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = HyperbolicSecantGenerator.create(2.0, 1.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean = mu = 2.0
        assertEquals(2.0, mean, 0.1)
    }
}