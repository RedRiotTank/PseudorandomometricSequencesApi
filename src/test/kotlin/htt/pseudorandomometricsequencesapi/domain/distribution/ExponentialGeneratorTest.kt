package htt.pseudorandomometricsequencesapi.domain.distribution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Random

class FakeJavaRandomForExponential : Random() {
    // nextDouble is overridden to provide a predictable value (non-zero to avoid log(0))
    override fun nextDouble() = 0.5
    override fun nextLong() = 1L
}

class ExponentialGeneratorTest {

    private val fakeRandom = FakeJavaRandomForExponential()


    @Test
    fun `create should use default value (lambda=1 0) when parameter is null`() {
        val generator = ExponentialGenerator.create(null, fakeRandom)
        assertNotNull(generator)
        // Expected sample for lambda=1.0 and U=0.5: -1/1.0 * ln(1 - 0.5)
        val expected = -1.0 * kotlin.math.ln(0.5)
        assertTrue(generator.sample() == expected)
    }

    @ParameterizedTest(name = "ExponentialGenerator should fail with lambda: {0}")
    @CsvSource("0.0", "-0.1") // Lambda (rate) must be positive
    fun `should throw exception if lambda is not positive`(lambda: Double) {
        assertThrows<IllegalArgumentException> {
            ExponentialGenerator.create(lambda, fakeRandom)
        }
    }

    @Test
    fun `sample should use the inverse transform formula correctly`() {
        val lambda = 2.0
        val generator = ExponentialGenerator.create(lambda, fakeRandom)

        // U = 0.5 (from fakeRandom)
        // Formula: X = -1/lambda * ln(1 - U)
        val expected = -1.0 / lambda * kotlin.math.ln(1.0 - 0.5)

        val sampleValue = generator.sample()

        assertNotNull(sampleValue)
        assertTrue(sampleValue > 0.0) // An exponential distribution sample must always be non-negative
        assertEquals(expected, sampleValue, 1e-9) // Floating point precision check
    }
}
