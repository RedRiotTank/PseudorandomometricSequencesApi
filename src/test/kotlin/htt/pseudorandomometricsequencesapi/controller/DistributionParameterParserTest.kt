package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DistributionParameterParserTest {

    @Autowired
    private lateinit var factories: List<DistributionFactory>

    @Test
    fun `should parse a Gaussian description`() {
        val parameters = DistributionParameterParser.parse(
            "param1 = mean μ (def. 0.0), param2 = std. deviation σ (def. 1.0)"
        )

        assertEquals(2, parameters.size)
        assertEquals(1, parameters[0].position)
        assertEquals("mean", parameters[0].label)
        assertEquals("μ", parameters[0].symbol)
        assertEquals(0.0, parameters[0].defaultValue)
        assertFalse(parameters[0].integer)
        assertEquals("std. deviation", parameters[1].label)
        assertEquals("σ", parameters[1].symbol)
        assertEquals(1.0, parameters[1].defaultValue)
    }

    @Test
    fun `should mark integer parameters`() {
        val parameters = DistributionParameterParser.parse(
            "param1 = trials n [integer] (def. 10), param2 = probability p (def. 0.5)"
        )

        assertEquals(2, parameters.size)
        assertTrue(parameters[0].integer)
        assertEquals("trials", parameters[0].label)
        assertEquals("n", parameters[0].symbol)
        assertFalse(parameters[1].integer)
        assertEquals("probability", parameters[1].label)
        assertEquals("p", parameters[1].symbol)
    }

    @Test
    fun `should keep a parameter without a symbol`() {
        val parameters = DistributionParameterParser.parse(
            "param1 = min a (def. 0.0), param2 = max d (def. 1.0), " +
                "param3 = plateau fraction in (0,1] (def. 0.5; 1=Uniform, →0=Triangular)"
        )

        assertEquals(3, parameters.size)
        assertEquals("a", parameters[0].symbol)
        assertEquals("d", parameters[1].symbol)
        assertEquals("plateau fraction", parameters[2].label)
        assertEquals(null, parameters[2].symbol)
        assertEquals(0.5, parameters[2].defaultValue)
    }

    @Test
    fun `should extract symbols from constrained and compound labels`() {
        val parameters = DistributionParameterParser.parse(
            "param1 = mean μ (def. 0.0), param2 = std. deviation σ (def. 1.0), " +
                "param3 = half-width in σ units w (def. 3.0)"
        )

        assertEquals("w", parameters[2].symbol)
        assertEquals("half-width in σ units", parameters[2].label)

        val temperature = DistributionParameterParser.parse(
            "param1 = dimensionless temperature θ = kT/mc² (def. 1.0)"
        )
        assertEquals("θ", temperature[0].symbol)
        assertEquals("dimensionless temperature", temperature[0].label)

        val fraction = DistributionParameterParser.parse(
            "param1 = ε/kT (def. 1.0; can be negative) — returns 0 or 1"
        )
        assertEquals("ε/kT", fraction[0].symbol)
    }

    @Test
    fun `should return no parameters for distributions without parameters`() {
        assertTrue(
            DistributionParameterParser.parse(
                "no parameters — returns leading digit in {1,...,9} following Benford's law"
            ).isEmpty()
        )
    }

    @Test
    fun `should parse every registered distribution description`() {
        factories.forEach { factory ->
            val expectedPositions = Regex("""param(\d)\s*=""")
                .findAll(factory.description)
                .map { it.groupValues[1].toInt() }
                .sorted()
                .toList()

            val parameters = DistributionParameterParser.parse(factory.description)

            assertEquals(expectedPositions, parameters.map { it.position }, factory.name)
            assertTrue(parameters.size <= 3, "${factory.name} has more than three parameters")
            assertTrue(parameters.all { it.label.isNotBlank() }, "${factory.name} has a blank parameter label")
        }
    }
}
