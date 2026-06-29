package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.GaussianFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.UniformFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.BeforeEach

class RandomServiceTest {

    private lateinit var randomService: RandomService

    @BeforeEach
    fun setUp() {
        randomService = RandomService(listOf(UniformFactory(), GaussianFactory()))
    }

    @ParameterizedTest(name = "Should fail with count: {0}")
    @CsvSource("0", "-10")
    fun `should throw IllegalArgumentException if count is not positive`(invalidCount: Int) {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequence(invalidCount, "general", "uniform", 0.0, 1.0)
        }
        assertEquals("Count must be positive.", exception.message)
    }

    @Test
    fun `should throw IllegalArgumentException if count is greater than 2_000_000`() {
        val largeCount = 2000001
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequence(largeCount, "general", "uniform", 0.0, 1.0)
        }
        assertEquals("Count cannot be greater than 2,000,000", exception.message)
    }

    @ParameterizedTest(name = "Should process generator type: {0}")
    @CsvSource("secure", "general", "SECURE", "GeNeRaL")
    fun `should process valid generator types`(validType: String) {
        val result = randomService.generateSequence(1, validType, "uniform", 1.0, 2.0)
        assertNotNull(result)
        assertEquals(1, result.sequence.size)
    }

    @Test
    fun `should throw IllegalArgumentException for invalid generator type`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequence(1, "invalid", "uniform", 0.0, 1.0)
        }
        assertEquals("Invalid type. Use 'secure' or 'general'.", exception.message)
    }

    @Test
    fun `should throw IllegalArgumentException for invalid distribution`() {
        val invalidDistribution = "test"
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequence(10, "general", invalidDistribution)
        }
        assertEquals("Invalid Distribution: $invalidDistribution. Use one of the supported types.", exception.message)
    }

    @Test
    fun `should generate a sequence of the specified length for Uniform`() {
        val expectedCount = 5
        val result = randomService.generateSequence(
            count = expectedCount,
            type = "general",
            distribution = "uniform",
            param1 = 10.0,
            param2 = 20.0
        )

        assertNotNull(result)
        assertEquals(expectedCount, result.sequence.size)
    }

    @Test
    fun `should generate a sequence of the specified length for Gaussian`() {
        val expectedCount = 10
        val result = randomService.generateSequence(
            count = expectedCount,
            type = "secure",
            distribution = "gaussian",
            param1 = 0.0,
            param2 = 1.0
        )

        assertNotNull(result)
        assertEquals(expectedCount, result.sequence.size)
    }

    @Test
    fun `should propagate IllegalArgumentException when required parameters are missing for distribution`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequence(1, "general", "uniform", param1 = 1.0, param2 = null)
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `should generate identical sequences for the same seed`() {
        val seed = 123456L
        val result1 = randomService.generateSequence(100, "general", "uniform", 0.0, 1.0, seed = seed)
        val result2 = randomService.generateSequence(100, "general", "uniform", 0.0, 1.0, seed = seed)
        assertEquals(result1.sequence, result2.sequence)
        assertEquals(seed, result1.seed)
        assertEquals(seed, result2.seed)
    }

    @Test
    fun `should generate identical sequences for the same seed with secure generator`() {
        val seed = 987654321L
        val result1 = randomService.generateSequence(100, "secure", "gaussian", 0.0, 1.0, seed = seed)
        val result2 = randomService.generateSequence(100, "secure", "gaussian", 0.0, 1.0, seed = seed)
        assertEquals(result1.sequence, result2.sequence)
        assertEquals(seed, result1.seed)
        assertEquals(seed, result2.seed)
    }

    @Test
    fun `should generate different sequences when different seeds are provided`() {
        val result1 = randomService.generateSequence(100, "general", "uniform", 0.0, 1.0, seed = 11111L)
        val result2 = randomService.generateSequence(100, "general", "uniform", 0.0, 1.0, seed = 22222L)
        org.junit.jupiter.api.Assertions.assertNotEquals(result1.sequence, result2.sequence)
    }
}
