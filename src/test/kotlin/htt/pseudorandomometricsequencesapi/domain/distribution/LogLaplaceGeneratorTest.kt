package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FakeCommonsRandomForLogLaplace : RandomGenerator {
    override fun nextDouble() = 0.5
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0; override fun nextInt(n: Int) = 0; override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}; override fun setSeed(seed: Long) {}; override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false; override fun nextFloat() = 0.5f; override fun nextBytes(bytes: ByteArray) {}
}

class LogLaplaceGeneratorTest {
    private val fakeRandom = FakeCommonsRandomForLogLaplace()

    @Test fun `create should use default values (mu=0 0, b=1 0) when parameters are null`() {
        val gen = LogLaplaceGenerator.create(null, null, fakeRandom)
        assertNotNull(gen)
        // Laplace(0, 1) at U=0.5 → 0.0 → exp(0) = 1.0
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @ParameterizedTest(name = "LogLaplace should fail with b={1}")
    @CsvSource("0.0, 0.0", "0.0, -1.0")
    fun `should throw when log-scale is not positive`(mu: Double, b: Double) {
        assertThrows<IllegalArgumentException> { LogLaplaceGenerator.create(mu, b, fakeRandom) }
    }

    @Test fun `sample equals exp(Laplace(mu, b)) so with U=0 5 at center gives exp(mu)`() {
        // At U=0.5 Laplace CDF inverts to mu; exp(mu=0) = 1
        val gen = LogLaplaceGenerator.create(0.0, 1.0, fakeRandom)
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @Test fun `all samples should be strictly positive`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = LogLaplaceGenerator.create(0.0, 1.0, rng)
        repeat(10_000) { assertTrue(gen.sample() > 0.0) }
    }

    @Test fun `statistical median should be close to exp(mu)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val mu = 1.0
        val gen = LogLaplaceGenerator.create(mu, 1.0, rng)
        val samples = (1..50_000).map { gen.sample() }.sorted()
        val median = samples[25_000]
        // Theoretical median = exp(mu) = e
        val expectedMedian = kotlin.math.exp(mu)
        assertTrue(kotlin.math.abs(median - expectedMedian) < 0.1) {
            "Expected median close to $expectedMedian, but was $median"
        }
    }
}
       