package htt.pseudorandomometricsequencesapi.domain

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
        randomService = RandomService()
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
        assertEquals(1, result.size)
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
        val invalidDistribution = "poisson"
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
        assertEquals(expectedCount, result.size)
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
        assertEquals(expectedCount, result.size)
    }

    @Test
    fun `should propagate IllegalArgumentException when required parameters are missing for distribution`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequence(1, "general", "uniform", param1 = 1.0, param2 = null)
        }
        assertNotNull(exception.message)
    }
}
