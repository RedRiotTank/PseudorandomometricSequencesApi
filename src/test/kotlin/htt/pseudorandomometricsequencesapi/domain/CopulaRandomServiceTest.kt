package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.copula.CopulaVariableSpec
import htt.pseudorandomometricsequencesapi.domain.distribution.BetaFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.BinomialFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.GammaFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.GaussianFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.PoissonFactory
import htt.pseudorandomometricsequencesapi.domain.distribution.UniformFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt

class CopulaRandomServiceTest {

    private lateinit var service: CopulaRandomService

    @BeforeEach
    fun setUp() {
        service = CopulaRandomService(
            listOf(
                UniformFactory(),
                GaussianFactory(),
                GammaFactory(),
                BetaFactory(),
                BinomialFactory(),
                PoissonFactory()
            )
        )
    }

    private fun gaussian(name: String? = null) =
        CopulaVariableSpec(name = name, distribution = "gaussian", param1 = 0.0, param2 = 1.0)

    private fun gamma(name: String? = null) =
        CopulaVariableSpec(name = name, distribution = "gamma", param1 = 2.0, param2 = 1.0)

    private fun beta(name: String? = null) =
        CopulaVariableSpec(name = name, distribution = "beta", param1 = 2.0, param2 = 5.0)

    private fun binomial(name: String? = null) =
        CopulaVariableSpec(name = name, distribution = "binomial", param1 = 10.0, param2 = 0.5)

    private fun poisson(name: String? = null) =
        CopulaVariableSpec(name = name, distribution = "poisson", param1 = 3.0)

    private fun correlation(rho: Double) =
        listOf(listOf(1.0, rho), listOf(rho, 1.0))

    private fun ranks(values: List<Double>): DoubleArray {
        val sorted = values.withIndex().sortedBy { it.value }
        val ranks = DoubleArray(values.size)
        var index = 0
        while (index < sorted.size) {
            var end = index
            while (end + 1 < sorted.size && sorted[end + 1].value == sorted[index].value) {
                end++
            }
            val averageRank = ((index + 1) + (end + 1)) / 2.0
            for (position in index..end) {
                ranks[sorted[position].index] = averageRank
            }
            index = end + 1
        }
        return ranks
    }

    private fun pearson(first: DoubleArray, second: DoubleArray): Double {
        val firstMean = first.average()
        val secondMean = second.average()
        val covariance = first.indices.sumOf { index ->
            (first[index] - firstMean) * (second[index] - secondMean)
        }
        val firstVariance = first.sumOf { (it - firstMean) * (it - firstMean) }
        val secondVariance = second.sumOf { (it - secondMean) * (it - secondMean) }
        return covariance / sqrt(firstVariance * secondVariance)
    }

    private fun spearman(first: List<Double>, second: List<Double>): Double =
        pearson(ranks(first), ranks(second))

    private fun expectedSpearman(rho: Double): Double = 6.0 / PI * asin(rho / 2.0)

    @Test
    fun `should generate one sequence per variable with the requested length`() {
        val result = service.generate(
            count = 20,
            type = "general",
            variables = listOf(gaussian("x"), gamma("y")),
            correlation = correlation(0.5),
            seed = 42L,
            quantileSamples = 10_000
        )

        assertEquals(42L, result.seed)
        assertEquals(10_000, result.quantileSamples)
        assertEquals(2, result.variables.size)
        assertEquals(listOf("x", "y"), result.variables.map { it.name })
        assertEquals(listOf("gaussian", "gamma"), result.variables.map { it.distribution })
        assertTrue(result.variables.all { it.sequence.size == 20 })
    }

    @Test
    fun `should assign default variable names`() {
        val result = service.generate(
            count = 2,
            type = "general",
            variables = listOf(gaussian(), gamma()),
            correlation = correlation(0.25),
            seed = 1L,
            quantileSamples = 10_000
        )

        assertEquals(listOf("variable1", "variable2"), result.variables.map { it.name })
    }

    @Test
    fun `should be deterministic for the same seed`() {
        val variables = listOf(gaussian("x"), gamma("y"), beta("z"))
        val correlation = listOf(
            listOf(1.0, 0.6, 0.4),
            listOf(0.6, 1.0, 0.3),
            listOf(0.4, 0.3, 1.0)
        )

        val first = service.generate(
            25, "general", variables, correlation, seed = 1234L, quantileSamples = 10_000
        )
        val second = service.generate(
            25, "general", variables, correlation, seed = 1234L, quantileSamples = 10_000
        )

        assertEquals(first.seed, second.seed)
        assertEquals(first.correlation, second.correlation)
        assertEquals(first.variables.map { it.sequence }, second.variables.map { it.sequence })
    }

    @Test
    fun `should generate different sequences for different seeds`() {
        val variables = listOf(gaussian("x"), gamma("y"))

        val first = service.generate(
            20, "general", variables, correlation(0.6), seed = 111L, quantileSamples = 10_000
        )
        val second = service.generate(
            20, "general", variables, correlation(0.6), seed = 222L, quantileSamples = 10_000
        )

        assertNotEquals(first.variables.map { it.sequence }, second.variables.map { it.sequence })
    }

    @Test
    fun `should induce the requested dependence for normal marginals`() {
        val result = service.generate(
            count = 20_000,
            type = "general",
            variables = listOf(gaussian("x"), gaussian("y")),
            correlation = correlation(0.8),
            seed = 54321L,
            quantileSamples = 30_000
        )

        val x = result.variables[0].sequence
        val y = result.variables[1].sequence

        assertEquals(0.8, pearson(x.toDoubleArray(), y.toDoubleArray()), 0.06)
        assertEquals(expectedSpearman(0.8), spearman(x, y), 0.06)
        assertEquals(0.0, x.average(), 0.05)
        assertEquals(1.0, x.sumOf { (it - x.average()) * (it - x.average()) } / (x.size - 1), 0.08)
    }

    @Test
    fun `should induce rank dependence across different marginal distributions`() {
        val result = service.generate(
            count = 20_000,
            type = "general",
            variables = listOf(gamma("g"), beta("b")),
            correlation = correlation(0.7),
            seed = 24680L,
            quantileSamples = 30_000
        )

        val gammaValues = result.variables[0].sequence
        val betaValues = result.variables[1].sequence

        assertEquals(expectedSpearman(0.7), spearman(gammaValues, betaValues), 0.07)
        assertEquals(2.0, gammaValues.average(), 0.1)
        assertEquals(2.0 / 7.0, betaValues.average(), 0.02)
        assertTrue(gammaValues.all { it > 0.0 })
        assertTrue(betaValues.all { it in 0.0..1.0 })
    }

    @Test
    fun `should preserve discrete marginal supports`() {
        val result = service.generate(
            count = 20_000,
            type = "general",
            variables = listOf(binomial("b"), poisson("p")),
            correlation = correlation(0.8),
            seed = 13579L,
            quantileSamples = 30_000
        )

        val binomialValues = result.variables[0].sequence
        val poissonValues = result.variables[1].sequence

        assertTrue(binomialValues.all { it >= 0.0 && it <= 10.0 && it % 1.0 == 0.0 })
        assertTrue(poissonValues.all { it >= 0.0 && it % 1.0 == 0.0 })
        assertEquals(expectedSpearman(0.8), spearman(binomialValues, poissonValues), 0.08)
    }

    @Test
    fun `should generate independent variables for the identity correlation matrix`() {
        val result = service.generate(
            count = 20_000,
            type = "general",
            variables = listOf(gaussian("x"), gaussian("y")),
            correlation = correlation(0.0),
            seed = 999L,
            quantileSamples = 30_000
        )

        val correlationValue = pearson(
            result.variables[0].sequence.toDoubleArray(),
            result.variables[1].sequence.toDoubleArray()
        )

        assertTrue(abs(correlationValue) < 0.05)
    }

    @Test
    fun `should support a single variable`() {
        val result = service.generate(
            count = 5,
            type = "general",
            variables = listOf(gaussian("only")),
            correlation = listOf(listOf(1.0)),
            seed = 7L,
            quantileSamples = 10_000
        )

        assertEquals(1, result.variables.size)
        assertEquals("only", result.variables[0].name)
        assertEquals(5, result.variables[0].sequence.size)
    }

    @Test
    fun `should normalize type and distribution names`() {
        val result = service.generate(
            count = 3,
            type = "GENERAL",
            variables = listOf(CopulaVariableSpec("x", "GAUSSIAN", 0.0, 1.0)),
            correlation = listOf(listOf(1.0)),
            seed = 1L,
            quantileSamples = 10_000
        )

        assertEquals("gaussian", result.variables[0].distribution)
    }

    @Test
    fun `should reject an empty variable list`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(1, "general", emptyList(), listOf(listOf(1.0)))
        }

        assertEquals("At least one variable must be specified.", exception.message)
    }

    @Test
    fun `should reject duplicate variable names`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("same"), gamma("same")),
                correlation(0.5),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Variable names must be unique.", exception.message)
    }

    @Test
    fun `should reject blank variable names`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("   ")),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Variable names must not be blank.", exception.message)
    }

    @Test
    fun `should reject a correlation matrix with the wrong number of rows`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x"), gamma("y")),
                listOf(listOf(1.0, 0.5)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals(
            "Correlation matrix must have 2 rows (one per variable).",
            exception.message
        )
    }

    @Test
    fun `should reject a non-square correlation matrix`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x"), gamma("y")),
                listOf(listOf(1.0, 0.5), listOf(0.5)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals(
            "Correlation matrix must be a square 2x2 matrix.",
            exception.message
        )
    }

    @Test
    fun `should reject an asymmetric correlation matrix`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x"), gamma("y")),
                listOf(listOf(1.0, 0.2), listOf(0.5, 1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Correlation matrix must be symmetric.", exception.message)
    }

    @Test
    fun `should reject a diagonal other than one`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x"), gamma("y")),
                listOf(listOf(0.9, 0.2), listOf(0.2, 1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Correlation matrix diagonal must be 1.0.", exception.message)
    }

    @Test
    fun `should reject correlation entries outside minus one and one`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x"), gamma("y")),
                listOf(listOf(1.0, 1.2), listOf(1.2, 1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals(
            "Correlation matrix entries must be between -1 and 1.",
            exception.message
        )
    }

    @Test
    fun `should reject a correlation matrix that is not positive definite`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x"), gaussian("y")),
                listOf(listOf(1.0, 1.0), listOf(1.0, 1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertTrue(exception.message!!.contains("positive definite"))
    }

    @ParameterizedTest(name = "Should fail with count: {0}")
    @CsvSource("0", "-10")
    fun `should reject a non-positive count`(invalidCount: Int) {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                invalidCount,
                "general",
                listOf(gaussian("x")),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Count must be positive.", exception.message)
    }

    @Test
    fun `should reject a count greater than the per-request limit`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                2_000_001,
                "general",
                listOf(gaussian("x")),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Count cannot be greater than 2,000,000", exception.message)
    }

    @Test
    fun `should enforce the total number of returned samples`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1_500_000,
                "general",
                listOf(gaussian("x"), gamma("y")),
                correlation(0.5),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals(
            "Total samples across all variables cannot be greater than 2,000,000",
            exception.message
        )
    }

    @Test
    fun `should reject quantile sample counts outside the allowed range`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(gaussian("x")),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 1_000
            )
        }

        assertEquals(
            "quantileSamples must be between 10,000 and 1,000,000.",
            exception.message
        )
    }

    @Test
    fun `should enforce the total quantile sample budget`() {
        val variables = List(21) { index -> gaussian("x$index") }
        val correlation = List(21) { row ->
            List(21) { column -> if (row == column) 1.0 else 0.0 }
        }

        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                variables,
                correlation,
                seed = 1L,
                quantileSamples = 1_000_000
            )
        }

        assertEquals(
            "Total quantile samples across all variables cannot be greater than 20,000,000.",
            exception.message
        )
    }

    @Test
    fun `should reject an invalid distribution`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(CopulaVariableSpec("x", "not-a-distribution")),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals(
            "Invalid Distribution: not-a-distribution. Use one of the supported types.",
            exception.message
        )
    }

    @Test
    fun `should propagate marginal parameter validation errors`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "general",
                listOf(CopulaVariableSpec("x", "gaussian", 0.0, 0.0)),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Gaussian 'stddev' (param2) must be positive.", exception.message)
    }

    @Test
    fun `should reject an invalid generator type`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.generate(
                1,
                "invalid",
                listOf(gaussian("x")),
                listOf(listOf(1.0)),
                seed = 1L,
                quantileSamples = 10_000
            )
        }

        assertEquals("Invalid type. Use 'secure' or 'general'.", exception.message)
    }
}
