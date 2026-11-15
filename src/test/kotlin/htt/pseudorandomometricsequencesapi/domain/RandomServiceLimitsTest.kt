package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import htt.pseudorandomometricsequencesapi.domain.distribution.UniformFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RandomServiceLimitsTest {

    private fun limits(
        maxSamples: Int = 100,
        maxValues: Int = 1_000,
        maxVariables: Int = 10
    ) = ApiLimitsProperties().apply {
        maxSamplesPerRequest = maxSamples
        maxValuesPerRequest = maxValues
        maxVariablesPerRequest = maxVariables
        maxGenerationMillis = 5_000
        calibrationSamples = 16
    }

    @Test
    fun `should use the configured sample limit in validation and messages`() {
        val service = RandomService(listOf(UniformFactory()), limits(maxSamples = 7))

        val exception = assertThrows<IllegalArgumentException> {
            service.generateSequence(8, "general", "uniform")
        }

        assertEquals("Count cannot be greater than 7", exception.message)
    }

    @Test
    fun `should use the configured total-values limit across variables`() {
        val service = RandomService(listOf(UniformFactory()), limits(maxValues = 50))

        val exception = assertThrows<IllegalArgumentException> {
            service.generateSequences(
                count = 30,
                type = "general",
                variables = listOf(
                    VariableSpec("a", "uniform"),
                    VariableSpec("b", "uniform")
                )
            )
        }

        assertEquals("Total samples across all variables cannot be greater than 50", exception.message)
    }

    @Test
    fun `should reject more variables than the configured maximum`() {
        val service = RandomService(listOf(UniformFactory()), limits(maxVariables = 2))

        val exception = assertThrows<IllegalArgumentException> {
            service.generateSequences(
                count = 1,
                type = "general",
                variables = listOf(
                    VariableSpec("a", "uniform"),
                    VariableSpec("b", "uniform"),
                    VariableSpec("c", "uniform")
                )
            )
        }

        assertEquals("Too many variables: 3. The maximum is 2.", exception.message)
    }

    @Test
    fun `should reject variable names longer than the configured maximum`() {
        val service = RandomService(
            listOf(UniformFactory()),
            limits().apply { maxVariableNameLength = 5 }
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.generateSequences(
                count = 1,
                type = "general",
                variables = listOf(VariableSpec("toolong", "uniform"))
            )
        }

        assertEquals("Variable names must not be longer than 5 characters.", exception.message)
    }

    @Test
    fun `should reject requests whose estimated generation time exceeds the budget`() {
        val service = RandomService(
            listOf(UniformFactory()),
            limits(maxSamples = 2_000_000).apply {
                maxGenerationMillis = 1
                calibrationSamples = 1
            }
        )

        assertThrows<GenerationBudgetExceededException> {
            service.generateSequence(2_000_000, "secure", "uniform")
        }
    }
}
