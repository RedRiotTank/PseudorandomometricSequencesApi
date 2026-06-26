package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.sqrt

/**
 * For Kumaraswamy with a=2, b=2:
 * X = (1 - (1-U)^(1/b))^(1/a) = (1 - 0.5^0.5)^0.5 = (1 - 1/sqrt(2))^0.5
 */
class FakeCommonsRandomForKumaraswamy : RandomGenerator {
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

class KumaraswamyGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForKumaraswamy()

    @Test
    fun `create should use default values (a=2 0, b=2 0) when parameters are null`() {
        val generator = KumaraswamyGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)
        val sample = generator.sample()
        assertTrue(sample > 0.0 && sample < 1.0) { "Kumaraswamy sample must be in (0,1), got $sample" }
    }

    @ParameterizedTest(name = "KumaraswamyGenerator should fail with a: {0} or b: {1}")
    @CsvSource(
        "0.0, 1.0",    // a <= 0
        "-1.0, 1.0",   // a negative
        "1.0, 0.0",    // b <= 0
        "1.0, -1.0"    // b negative
    )
    fun `should throw exception for invalid parameters`(a: Double, b: Double) {
        assertThrows<IllegalArgumentException> {
            KumaraswamyGenerator.create(a, b, fakeRandom)
        }
    }

    @Test
    fun `sample should calculate X = (1-(1-U) to 1 over b) to 1 over a correctly`() {
        val generator = KumaraswamyGenerator.create(2.0, 2.0, fakeRandom)
        // U=0.5, a=2, b=2: X = (1 - (1-0.5)^(1/2))^(1/2) = (1 - 0.5^0.5)^0.5 = (1 - 1/sqrt(2))^0.5
        val expected = sqrt(1.0 - sqrt(0.5))
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @Test
    fun `all samples should be in (0, 1)`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val generator = KumaraswamyGenerator.create(2.0, 2.0, rng)
        val N = 50_000
        val samples = (1..N).map { generator.sample() }
        assertTrue(samples.all { it > 0.0 && it < 1.0 }) {
            "All Kumaraswamy samples must be in (0, 1)"
        }
    }
}
