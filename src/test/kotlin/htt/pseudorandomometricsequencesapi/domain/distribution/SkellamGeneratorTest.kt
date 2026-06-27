package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class FakeCommonsRandomForSkellam : RandomGenerator {
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

class SkellamGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForSkellam()

    @Test
    fun `create should use default values (mu1=1 0, mu2=1 0) when parameters are null`() {
        val gen = SkellamGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
    }

    @ParameterizedTest(name = "Skellam should fail with mu1={0}, mu2={1}")
    @CsvSource("0.0, 1.0", "-1.0, 1.0", "1.0, 0.0", "1.0, -1.0")
    fun `should throw when mu1 or mu2 is not positive`(mu1: Double, mu2: Double) {
        assertThrows<IllegalArgumentException> { SkellamGenerator.create(mu1, mu2, fakeRandom) }
    }

    @Test
    fun `output should be an integer`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = SkellamGenerator.create(2.0, 1.0, rng)
        repeat(1_000) { assertTrue(gen.sample() % 1.0 == 0.0) }
    }

    @Test
    fun `statistical mean should be close to mu1 - mu2`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu1 = 5.0; val mu2 = 2.0
        val gen = SkellamGenerator.create(mu1, mu2, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean = mu1 - mu2 = 3.0
        assertEquals(3.0, mean, 0.1)
    }

    @Test
    fun `statistical variance should be close to mu1 + mu2`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu1 = 3.0; val mu2 = 2.0
        val gen = SkellamGenerator.create(mu1, mu2, rng)
        val samples = (1..50_000).map { gen.sample() }
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()
        // Theoretical variance = mu1 + mu2 = 5.0
        assertEquals(5.0, variance, 0.3)
    }

    @Test
    fun `with equal mu1 and mu2 mean should be close to 0`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = SkellamGenerator.create(3.0, 3.0, rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        // Theoretical mean for equal mu1 and mu2 is 0.0
        assertEquals(0.0, mean, 0.1)
    }
}