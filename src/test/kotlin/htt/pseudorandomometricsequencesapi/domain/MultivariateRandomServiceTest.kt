package htt.pseudorandomometricsequencesapi.domain

import htt.pseudorandomometricsequencesapi.domain.multivariate.DirichletFactory
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultinomialFactory
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateNormalFactory
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSpec
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateTFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class MultivariateRandomServiceTest {

    private lateinit var randomService: MultivariateRandomService

    @BeforeEach
    fun setUp() {
        randomService = MultivariateRandomService(
            listOf(
                MultivariateNormalFactory(),
                MultivariateTFactory(),
                DirichletFactory(),
                MultinomialFactory()
            )
        )
    }

    private fun normalSpec(
        mean: List<Double> = listOf(0.0, 0.0),
        covariance: List<List<Double>> = listOf(listOf(1.0, 0.8), listOf(0.8, 1.0)),
        names: List<String>? = null
    ) = MultivariateSpec(
        distribution = "multivariate-normal",
        mean = mean,
        covariance = covariance,
        variableNames = names
    )

    private fun mean(values: List<Double>): Double = values.average()

    private fun covariance(first: List<Double>, second: List<Double>): Double {
        val firstMean = first.average()
        val secondMean = second.average()
        val divisor = first.size - 1
        return first.indices.sumOf { index ->
            (first[index] - firstMean) * (second[index] - secondMean)
        } / divisor
    }

    @Test
    fun `should generate the requested number of vector samples`() {
        val result = randomService.generate(
            count = 10,
            type = "general",
            spec = normalSpec(),
            seed = 42L
        )

        assertEquals("multivariate-normal", result.distribution)
        assertEquals(42L, result.seed)
        assertEquals(listOf("x1", "x2"), result.variableNames)
        assertEquals(10, result.sequence.size)
        assertTrue(result.sequence.all { it.size == 2 })
    }

    @Test
    fun `should reproduce joint samples for the same seed`() {
        val first = randomService.generate(50, "general", normalSpec(), seed = 123L)
        val second = randomService.generate(50, "general", normalSpec(), seed = 123L)

        assertEquals(first.sequence, second.sequence)
        assertEquals(first.variableNames, second.variableNames)
    }

    @Test
    fun `should generate different joint samples for different seeds`() {
        val first = randomService.generate(50, "general", normalSpec(), seed = 123L)
        val second = randomService.generate(50, "general", normalSpec(), seed = 456L)

        assertNotEquals(first.sequence, second.sequence)
    }

    @Test
    fun `should produce multivariate normal components matching the requested covariance`() {
        val result = randomService.generate(
            count = 20_000,
            type = "general",
            spec = normalSpec(),
            seed = 98765L
        )

        val x = result.sequence.map { it[0] }
        val y = result.sequence.map { it[1] }

        assertEquals(0.0, mean(x), 0.05)
        assertEquals(0.0, mean(y), 0.05)
        assertEquals(1.0, covariance(x, x), 0.08)
        assertEquals(1.0, covariance(y, y), 0.08)
        assertEquals(0.8, covariance(x, y), 0.08)
    }

    @Test
    fun `should default the multivariate normal mean to zero`() {
        val spec = MultivariateSpec(
            distribution = "multivariate-normal",
            covariance = listOf(listOf(1.0, 0.0), listOf(0.0, 1.0))
        )

        val result = randomService.generate(5_000, "general", spec, seed = 111L)
        val componentMean = result.sequence.map { it[0] }.average()

        assertEquals(0.0, componentMean, 0.05)
    }

    @Test
    fun `should support custom component names`() {
        val result = randomService.generate(
            count = 3,
            type = "general",
            spec = normalSpec(names = listOf("height", "weight")),
            seed = 1L
        )

        assertEquals(listOf("height", "weight"), result.variableNames)
    }

    @Test
    fun `should reject component names whose count does not match the dimension`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                normalSpec(names = listOf("only-one")),
                seed = 1L
            )
        }

        assertEquals(
            "Number of variable names (1) must match the distribution dimension (2).",
            exception.message
        )
    }

    @Test
    fun `should reject duplicate component names`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                normalSpec(names = listOf("same", "same")),
                seed = 1L
            )
        }

        assertEquals("Variable names must be unique.", exception.message)
    }

    @Test
    fun `should reject a covariance matrix that is not positive definite`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                normalSpec(covariance = listOf(listOf(1.0, 2.0), listOf(2.0, 1.0))),
                seed = 1L
            )
        }

        assertTrue(exception.message!!.contains("positive definite"))
    }

    @Test
    fun `should reject a non-symmetric covariance matrix`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                normalSpec(covariance = listOf(listOf(1.0, 0.2), listOf(0.5, 1.0))),
                seed = 1L
            )
        }

        assertEquals("Multivariate normal 'covariance' must be symmetric.", exception.message)
    }

    @Test
    fun `should reject a covariance matrix with mismatched dimensions`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                normalSpec(covariance = listOf(listOf(1.0, 0.0), listOf(0.0, 1.0), listOf(0.0, 0.0))),
                seed = 1L
            )
        }

        assertEquals("Multivariate normal 'covariance' must have 2 rows.", exception.message)
    }

    @Test
    fun `should generate multivariate t samples with the requested location`() {
        val result = randomService.generate(
            count = 10_000,
            type = "general",
            spec = MultivariateSpec(
                distribution = "multivariate-t",
                mean = listOf(1.0, 2.0),
                covariance = listOf(listOf(1.0, 0.5), listOf(0.5, 2.0)),
                degreesOfFreedom = 10.0
            ),
            seed = 777L
        )

        assertEquals(10_000, result.sequence.size)
        assertTrue(result.sequence.all { it.size == 2 })
        assertEquals(1.0, mean(result.sequence.map { it[0] }), 0.1)
        assertEquals(2.0, mean(result.sequence.map { it[1] }), 0.15)
    }

    @Test
    fun `should produce correlated multivariate t components`() {
        val result = randomService.generate(
            count = 20_000,
            type = "general",
            spec = MultivariateSpec(
                distribution = "multivariate-t",
                covariance = listOf(listOf(1.0, 0.5), listOf(0.5, 1.0)),
                degreesOfFreedom = 10.0
            ),
            seed = 888L
        )

        val x = result.sequence.map { it[0] }
        val y = result.sequence.map { it[1] }

        // For nu > 2, Var(X) = nu / (nu - 2) * scale and Cov(X, Y) scales identically.
        val factor = 10.0 / 8.0
        assertEquals(factor, covariance(x, x), 0.15)
        assertEquals(0.5 * factor, covariance(x, y), 0.15)
    }

    @Test
    fun `should reject non-positive multivariate t degrees of freedom`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                MultivariateSpec(
                    distribution = "multivariate-t",
                    covariance = listOf(listOf(1.0, 0.0), listOf(0.0, 1.0)),
                    degreesOfFreedom = 0.0
                ),
                seed = 1L
            )
        }

        assertEquals(
            "Multivariate t 'degreesOfFreedom' must be positive and finite.",
            exception.message
        )
    }

    @Test
    fun `should generate Dirichlet probability vectors that sum to one`() {
        val concentration = listOf(2.0, 5.0, 3.0)
        val result = randomService.generate(
            count = 2_000,
            type = "general",
            spec = MultivariateSpec(
                distribution = "dirichlet",
                concentration = concentration
            ),
            seed = 2468L
        )

        assertEquals(listOf("x1", "x2", "x3"), result.variableNames)
        assertTrue(result.sequence.all { sample -> abs(sample.sum() - 1.0) <= 1e-9 })
        assertTrue(result.sequence.all { sample -> sample.all { it >= 0.0 } })

        val total = concentration.sum()
        concentration.forEachIndexed { index, alpha ->
            assertEquals(alpha / total, mean(result.sequence.map { it[index] }), 0.03)
        }
    }

    @Test
    fun `should reject invalid Dirichlet concentration values`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                MultivariateSpec("dirichlet", concentration = listOf(1.0, 0.0)),
                seed = 1L
            )
        }

        assertEquals(
            "Dirichlet 'concentration' values must be positive and finite.",
            exception.message
        )
    }

    @Test
    fun `should generate multinomial counts that sum to the number of trials`() {
        val trials = 50
        val probabilities = listOf(0.2, 0.3, 0.5)
        val result = randomService.generate(
            count = 2_000,
            type = "general",
            spec = MultivariateSpec(
                distribution = "multinomial",
                trials = trials,
                probabilities = probabilities
            ),
            seed = 13579L
        )

        assertTrue(result.sequence.all { sample -> sample.sum() == trials.toDouble() })
        assertTrue(result.sequence.all { sample -> sample.all { it >= 0.0 && it % 1.0 == 0.0 } })

        probabilities.forEachIndexed { index, probability ->
            assertEquals(probability * trials, mean(result.sequence.map { it[index] }), 1.2)
        }
    }

    @Test
    fun `should reject multinomial probabilities that do not sum to one`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                MultivariateSpec("multinomial", trials = 10, probabilities = listOf(0.2, 0.2)),
                seed = 1L
            )
        }

        assertTrue(exception.message!!.contains("must sum to 1.0"))
    }

    @Test
    fun `should reject negative multinomial trials`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                MultivariateSpec("multinomial", trials = -1, probabilities = listOf(1.0)),
                seed = 1L
            )
        }

        assertEquals("Multinomial 'trials' must be non-negative.", exception.message)
    }

    @Test
    fun `should normalize generator type and distribution names`() {
        val result = randomService.generate(
            count = 3,
            type = "GENERAL",
            spec = MultivariateSpec(
                "MULTIVARIATE-NORMAL",
                covariance = listOf(listOf(1.0, 0.0), listOf(0.0, 1.0))
            ),
            seed = 1L
        )

        assertEquals("multivariate-normal", result.distribution)
        assertEquals(3, result.sequence.size)
    }

    @Test
    fun `should reject an invalid multivariate distribution`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(
                1,
                "general",
                MultivariateSpec("not-a-distribution"),
                seed = 1L
            )
        }

        assertEquals(
            "Invalid Multivariate Distribution: not-a-distribution. Use one of the supported types.",
            exception.message
        )
    }

    @ParameterizedTest(name = "Should fail with count: {0}")
    @CsvSource("0", "-10")
    fun `should reject a non-positive count`(invalidCount: Int) {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(invalidCount, "general", normalSpec(), seed = 1L)
        }

        assertEquals("Count must be positive.", exception.message)
    }

    @Test
    fun `should reject a count greater than the per-request limit`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(2_000_001, "general", normalSpec(), seed = 1L)
        }

        assertEquals("Count cannot be greater than 2,000,000", exception.message)
    }

    @Test
    fun `should enforce the total number of generated values`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(1_500_000, "general", normalSpec(), seed = 1L)
        }

        assertEquals(
            "Total number of generated values cannot be greater than 2,000,000",
            exception.message
        )
    }

    @Test
    fun `should reject an invalid generator type`() {
        val exception = assertThrows<IllegalArgumentException> {
            randomService.generate(1, "invalid", normalSpec(), seed = 1L)
        }

        assertEquals("Invalid type. Use 'secure' or 'general'.", exception.message)
    }
}
