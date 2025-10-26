package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FakeCommonsRandomForTStudent : RandomGenerator {
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
    override fun nextGaussian() = 0.0
}

class TStudentGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForTStudent()

    @Test
    fun `create should use default degrees of freedom (10 0) when parameter is null`() {
        val generator = TStudentGenerator.create(null, fakeRandom)
        assertNotNull(generator)

        assertEquals(10.0, generator.distribution.degreesOfFreedom)
    }


    @ParameterizedTest(name = "TStudentGenerator should fail with degreesOfFreedom: {0}")
    @ValueSource(doubles = [0.0, -1.0]) // Degrees of freedom (> 0)
    fun `should throw exception if degreesOfFreedom is not positive`(degreesOfFreedom: Double) {
        assertThrows<IllegalArgumentException> {
            TStudentGenerator.create(degreesOfFreedom, fakeRandom)
        }
    }

    @Test
    fun `sample should delegate to internal distribution`() {
        val generator = TStudentGenerator.create(5.0, fakeRandom)
        val sampleValue = generator.sample()

        assertNotNull(sampleValue)
    }
}
