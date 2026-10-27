package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.distribution.GaussianFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.UniformFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RandomServiceMultiVariableTest {

    private lateinit var randomService: RandomService

    @BeforeEach
    fun setUp() {
        randomService = RandomService(listOf(UniformFactory(), GaussianFactory()))
    }

    private fun uniform(name: String? = null, seed: Long? = null) =
        VariableSpec(name = name, distribution = "uniform", param1 = 0.0, param2 = 1.0, seed = seed)

    private fun gaussian(name: String? = null, seed: Long? = null) =
        VariableSpec(name = name, distribution = "gaussian", param1 = 0.0, param2 = 1.0, seed = seed)

    @Test
    fun `should generate one sequence per variable with the requested length`() {
        val result = randomService.generateSequences(
            count = 7,
            type = "general",
            variables = listOf(uniform("x"), gaussian("y"))
        )

        assertEquals(2, result.variables.size)
        assertEquals(listOf("x", "y"), result.variables.map { it.name })
        assertEquals(listOf("uniform", "gaussian"), result.variables.map { it.distribution })
        assertTrue(result.variables.all { it.sequence.size == 7 })
    }

    @Test
    fun `should assign default names in request order`() {
        val result = randomService.generateSequences(
            count = 2,
            type = "general",
            variables = listOf(uniform(), gaussian(), uniform())
        )

        assertEquals(listOf("variable1", "variable2", "variable3"), result.variables.map { it.name })
    }

    @Test
    fun `should return the request-level seed in the result`() {
        val result = randomService.generateSequences(
            count = 3,
            type = "general",
            variables = listOf(uniform("x")),
            seed = 424242L
        )

        assertEquals(424242L, result.seed)
    }

    @Test
    fun `should be deterministic for the same request seed`() {
        val variables = listOf(uniform("x"), gaussian("y"), uniform("z"))

        val first = randomService.generateSequences(25, "general", variables, seed = 123456L)
        val second = randomService.generateSequences(25, "general", variables, seed = 123456L)

        assertEquals(first.seed, second.seed)
        assertEquals(first.variables.map { it.seed }, second.variables.map { it.seed })
        assertEquals(first.variables.map { it.sequence }, second.variables.map { it.sequence })
    }

    @Test
    fun `should generate different sequences for different request seeds`() {
        val variables = listOf(uniform("x"), gaussian("y"))

        val first = randomService.generateSequences(20, "general", variables, seed = 11111L)
        val second = randomService.generateSequences(20, "general", variables, seed = 22222L)

        assertNotEquals(
            first.variables.map { it.sequence },
            second.variables.map { it.sequence }
        )
    }

    @Test
    fun `should derive independent seeds for variables using the same distribution`() {
        val result = randomService.generateSequences(
            count = 20,
            type = "general",
            variables = listOf(uniform("a"), uniform("b")),
            seed = 98765L
        )

        val first = result.variables[0]
        val second = result.variables[1]

        assertNotEquals(first.seed, second.seed)
        assertNotEquals(first.sequence, second.sequence)
    }

    @Test
    fun `should honor an explicit variable seed independently of the request seed`() {
        val variable = uniform("x", seed = 777L)

        val first = randomService.generateSequences(15, "general", listOf(variable), seed = 111L)
        val second = randomService.generateSequences(15, "general", listOf(variable), seed = 999L)

        assertEquals(777L, first.variables[0].seed)
        assertEquals(777L, second.variables[0].seed)
        assertEquals(first.variables[0].sequence, second.variables[0].sequence)
    }

    @Test
    fun `should keep derived seeds stable when another variable has an explicit seed`() {
        val withoutExplicit = randomService.generateSequences(
            5, "general", listOf(uniform("a"), uniform("b")), seed = 42L
        )
        val withExplicitFirst = randomService.generateSequences(
            5, "general", listOf(uniform("a", seed = 1L), uniform("b")), seed = 42L
        )

        assertEquals(withoutExplicit.variables[1].seed, withExplicitFirst.variables[1].seed)
        assertEquals(withoutExplicit.variables[1].sequence, withExplicitFirst.variables[1].sequence)
    }

    @Test
    fun `should normalize generator type and distribution names`() {
        val result = randomService.generateSequences(
            count = 3,
            type = "GENERAL",
            variables = listOf(VariableSpec("x", "UNIFORM", 0.0, 1.0))
        )

        assertEquals("uniform", result.variables[0].distribution)
        assertEquals(3, result.variables[0].sequence.size)
    }

    @Test
    fun `should reject an empty variable list`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(1, "general", emptyList())
        }

        assertEquals("At least one variable must be specified.", exception.message)
    }

    @Test
    fun `should reject duplicate variable names`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(
                1, "general", listOf(uniform("same"), gaussian("same"))
            )
        }

        assertEquals("Variable names must be unique.", exception.message)
    }

    @Test
    fun `should reject blank variable names`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(1, "general", listOf(uniform("   ")))
        }

        assertEquals("Variable names must not be blank.", exception.message)
    }

    @ParameterizedTest(name = "Should fail with count: {0}")
    @CsvSource("0", "-10")
    fun `should reject a non-positive count`(invalidCount: Int) {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(invalidCount, "general", listOf(uniform("x")))
        }

        assertEquals("Count must be positive.", exception.message)
    }

    @Test
    fun `should reject a count greater than the per-variable limit`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(2_000_001, "general", listOf(uniform("x")))
        }

        assertEquals("Count cannot be greater than 2,000,000", exception.message)
    }

    @Test
    fun `should enforce the total sample limit across variables`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(
                count = 1_500_000,
                type = "general",
                variables = listOf(uniform("a"), uniform("b"))
            )
        }

        assertEquals(
            "Total samples across all variables cannot be greater than 2,000,000",
            exception.message
        )
    }

    @Test
    fun `should reject an invalid generator type even when a seed is provided`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(1, "invalid", listOf(uniform("x")), seed = 1L)
        }

        assertEquals("Invalid type. Use 'secure' or 'general'.", exception.message)
    }

    @Test
    fun `should reject an invalid distribution`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(
                1,
                "general",
                listOf(VariableSpec("x", "not-a-distribution"))
            )
        }

        assertEquals(
            "Invalid Distribution: not-a-distribution. Use one of the supported types.",
            exception.message
        )
    }

    @Test
    fun `should propagate distribution parameter validation errors`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generateSequences(
                1,
                "general",
                listOf(VariableSpec("x", "gaussian", 0.0, 0.0))
            )
        }

        assertEquals("Gaussian 'stddev' (param2) must be positive.", exception.message)
    }

    @Test
    fun `should continue to support the single-variable service method`() {
        val result = randomService.generateSequence(
            count = 5,
            type = "general",
            distribution = "uniform",
            param1 = 10.0,
            param2 = 20.0,
            seed = 123L
        )

        assertEquals(5, result.sequence.size)
        assertEquals(123L, result.seed)
        assertTrue(result.sequence.all { it >= 10.0 && it <= 20.0 })
    }
}
