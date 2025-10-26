package htt.pseudorandomometricsequencesapi.domain.distribution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Random

class FakeJavaRandomForUniform : Random() {
    // nextDouble is mocked to return 0.5, which is U in X = min + (max - min) * U
    override fun nextDouble() = 0.5

    override fun nextLong() = 1L
}

class UniformGeneratorTest {

    private val fakeRandom = FakeJavaRandomForUniform()


    @Test
    fun `create should use default values (min=0 0, max=1 0) when parameters are null`() {
        val generator = UniformGenerator.create(null, null, fakeRandom)
        assertNotNull(generator)

        // Expected sample for min=0.0, max=1.0, and U=0.5: X = 0.0 + (1.0 - 0.0) * 0.5 = 0.5
        val expected = 0.5
        assertEquals(expected, generator.sample(), 1e-9)
    }

    @ParameterizedTest(name = "UniformGenerator should fail if min: {0} is not less than max: {1}")
    @CsvSource(
        "1.0, 1.0",     // min == max
        "2.0, 1.0"      // min > max
    )
    fun `should throw exception if min is greater than or equal to max`(min: Double, max: Double) {
        assertThrows<IllegalArgumentException> {
            UniformGenerator.create(min, max, fakeRandom)
        }
    }

    // --- sample method tests ---

    @Test
    fun `sample should calculate X = min + (max - min) U correctly`() {
        val min = 5.0
        val max = 15.0
        // U = 0.5 (from fakeRandom.nextDouble())

        val generator = UniformGenerator.create(min, max, fakeRandom)

        // Expected: X = 5.0 + (15.0 - 5.0) * 0.5 = 5.0 + 10.0 * 0.5 = 10.0
        val expected = 10.0

        val sampleValue = generator.sample()

        assertEquals(expected, sampleValue, 1e-9)
    }
}
