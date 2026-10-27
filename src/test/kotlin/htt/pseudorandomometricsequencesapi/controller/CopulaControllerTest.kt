package htt.pseudorandomometricsequencesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CopulaControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun performCopula(request: String) =
        mockMvc.perform(
            post("/api/v1/random/copula")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )

    @Test
    fun `should generate statistically dependent variables`() {
        performCopula(
            """
            {
              "count": 5,
              "type": "general",
              "seed": 42,
              "quantileSamples": 10000,
              "correlation": [[1.0, 0.8], [0.8, 1.0]],
              "variables": [
                {"name": "x", "distribution": "gaussian", "param1": 0, "param2": 1},
                {"name": "y", "distribution": "gaussian", "param1": 0, "param2": 1}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.type").value("general"))
            .andExpect(jsonPath("$.count").value(5))
            .andExpect(jsonPath("$.copula").value("gaussian"))
            .andExpect(jsonPath("$.marginalMethod").value("empirical-quantile"))
            .andExpect(jsonPath("$.seed").value(42))
            .andExpect(jsonPath("$.quantileSamples").value(10000))
            .andExpect(jsonPath("$.correlation[0][1]").value(0.8))
            .andExpect(jsonPath("$.variables.length()").value(2))
            .andExpect(jsonPath("$.variables[0].name").value("x"))
            .andExpect(jsonPath("$.variables[0].distribution").value("gaussian"))
            .andExpect(jsonPath("$.variables[0].sequence.length()").value(5))
            .andExpect(jsonPath("$.variables[1].name").value("y"))
            .andExpect(jsonPath("$.variables[1].sequence.length()").value(5))
    }

    @Test
    fun `should support different marginal distributions`() {
        performCopula(
            """
            {
              "count": 4,
              "seed": 7,
              "quantileSamples": 10000,
              "correlation": [[1.0, 0.6], [0.6, 1.0]],
              "variables": [
                {"name": "g", "distribution": "gamma", "param1": 2, "param2": 1},
                {"name": "b", "distribution": "beta", "param1": 2, "param2": 5}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.variables[0].distribution").value("gamma"))
            .andExpect(jsonPath("$.variables[1].distribution").value("beta"))
            .andExpect(jsonPath("$.variables[0].sequence.length()").value(4))
            .andExpect(jsonPath("$.variables[1].sequence.length()").value(4))
    }

    @Test
    fun `should return identical responses for the same seed`() {
        val request = """
            {
              "count": 10,
              "seed": 24680,
              "quantileSamples": 10000,
              "correlation": [[1.0, 0.7], [0.7, 1.0]],
              "variables": [
                {"name": "x", "distribution": "gaussian"},
                {"name": "y", "distribution": "gamma", "param1": 2, "param2": 1}
              ]
            }
            """.trimIndent()

        val first = performCopula(request).andExpect(status().isOk).andReturn()
            .response.contentAsString
        val second = performCopula(request).andExpect(status().isOk).andReturn()
            .response.contentAsString

        assertEquals(objectMapper.readTree(first), objectMapper.readTree(second))
    }

    @Test
    fun `should reject a non-positive-definite correlation matrix`() {
        performCopula(
            """
            {
              "count": 1,
              "quantileSamples": 10000,
              "correlation": [[1.0, 1.0], [1.0, 1.0]],
              "variables": [
                {"name": "x", "distribution": "gaussian"},
                {"name": "y", "distribution": "gaussian"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("positive definite")))
    }

    @Test
    fun `should reject an asymmetric correlation matrix`() {
        performCopula(
            """
            {
              "count": 1,
              "quantileSamples": 10000,
              "correlation": [[1.0, 0.2], [0.5, 1.0]],
              "variables": [
                {"name": "x", "distribution": "gaussian"},
                {"name": "y", "distribution": "gaussian"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Correlation matrix must be symmetric."))
    }

    @Test
    fun `should reject a request without variables`() {
        performCopula(
            """
            {
              "count": 1,
              "correlation": [[1.0]],
              "quantileSamples": 10000
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("At least one variable must be specified."))
    }

    @Test
    fun `should reject a request without a correlation matrix`() {
        performCopula(
            """
            {
              "count": 1,
              "variables": [
                {"name": "x", "distribution": "gaussian"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid or missing request body."))
    }

    @Test
    fun `should reject duplicate variable names`() {
        performCopula(
            """
            {
              "count": 1,
              "quantileSamples": 10000,
              "correlation": [[1.0, 0.3], [0.3, 1.0]],
              "variables": [
                {"name": "same", "distribution": "gaussian"},
                {"name": "same", "distribution": "gamma"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Variable names must be unique."))
    }

    @Test
    fun `should reject an invalid marginal distribution`() {
        performCopula(
            """
            {
              "count": 1,
              "quantileSamples": 10000,
              "correlation": [[1.0]],
              "variables": [
                {"name": "x", "distribution": "not-a-distribution"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Invalid Distribution")))
    }

    @Test
    fun `should reject an invalid quantile sample count`() {
        performCopula(
            """
            {
              "count": 1,
              "quantileSamples": 1000,
              "correlation": [[1.0]],
              "variables": [
                {"name": "x", "distribution": "gaussian"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("quantileSamples must be between")))
    }
}
