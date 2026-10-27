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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class MultivariateControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun performMultivariate(request: String) =
        mockMvc.perform(
            post("/api/v1/random/multivariate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )

    @Test
    fun `should generate a joint multivariate normal sequence`() {
        performMultivariate(
            """
            {
              "count": 5,
              "type": "general",
              "seed": 42,
              "distribution": "multivariate-normal",
              "mean": [0.0, 1.0],
              "covariance": [[1.0, 0.5], [0.5, 2.0]],
              "variableNames": ["a", "b"]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.type").value("general"))
            .andExpect(jsonPath("$.count").value(5))
            .andExpect(jsonPath("$.distribution").value("multivariate-normal"))
            .andExpect(jsonPath("$.dimension").value(2))
            .andExpect(jsonPath("$.seed").value(42))
            .andExpect(jsonPath("$.variableNames[0]").value("a"))
            .andExpect(jsonPath("$.variableNames[1]").value("b"))
            .andExpect(jsonPath("$.sequence.length()").value(5))
            .andExpect(jsonPath("$.sequence[0].length()").value(2))
    }

    @Test
    fun `should return identical multivariate responses for the same seed`() {
        val request = """
            {
              "count": 10,
              "seed": 24680,
              "distribution": "multivariate-normal",
              "covariance": [[1.0, 0.7], [0.7, 1.0]]
            }
            """.trimIndent()

        val first = performMultivariate(request).andExpect(status().isOk).andReturn()
            .response.contentAsString
        val second = performMultivariate(request).andExpect(status().isOk).andReturn()
            .response.contentAsString

        assertEquals(objectMapper.readTree(first), objectMapper.readTree(second))
    }

    @Test
    fun `should generate a Dirichlet sequence`() {
        performMultivariate(
            """
            {
              "count": 3,
              "distribution": "dirichlet",
              "concentration": [2.0, 5.0, 3.0]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.distribution").value("dirichlet"))
            .andExpect(jsonPath("$.dimension").value(3))
            .andExpect(jsonPath("$.sequence.length()").value(3))
            .andExpect(jsonPath("$.sequence[0].length()").value(3))
    }

    @Test
    fun `should generate a multinomial sequence`() {
        performMultivariate(
            """
            {
              "count": 2,
              "distribution": "multinomial",
              "trials": 10,
              "probabilities": [0.2, 0.8]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.distribution").value("multinomial"))
            .andExpect(jsonPath("$.dimension").value(2))
            .andExpect(jsonPath("$.sequence.length()").value(2))
            .andExpect(jsonPath("$.sequence[0].length()").value(2))
    }

    @Test
    fun `should generate a multivariate t sequence`() {
        performMultivariate(
            """
            {
              "count": 2,
              "distribution": "multivariate-t",
              "mean": [0.0, 0.0],
              "covariance": [[1.0, 0.0], [0.0, 1.0]],
              "degreesOfFreedom": 5.0
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.distribution").value("multivariate-t"))
            .andExpect(jsonPath("$.dimension").value(2))
            .andExpect(jsonPath("$.sequence.length()").value(2))
    }

    @Test
    fun `should reject a covariance matrix that is not positive definite`() {
        performMultivariate(
            """
            {
              "count": 1,
              "distribution": "multivariate-normal",
              "covariance": [[1.0, 2.0], [2.0, 1.0]]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("positive definite")))
    }

    @Test
    fun `should reject missing covariance parameters`() {
        performMultivariate(
            """
            {
              "count": 1,
              "distribution": "multivariate-normal"
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Multivariate normal requires 'covariance'."))
    }

    @Test
    fun `should reject an invalid multivariate distribution`() {
        performMultivariate(
            """
            {
              "count": 1,
              "distribution": "not-a-distribution"
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Invalid Multivariate Distribution")))
    }

    @Test
    fun `should reject a request without a distribution`() {
        performMultivariate("""{"count": 1}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid or missing request body."))
    }

    @Test
    fun `should reject component names with the wrong dimension`() {
        performMultivariate(
            """
            {
              "count": 1,
              "distribution": "multivariate-normal",
              "covariance": [[1.0, 0.0], [0.0, 1.0]],
              "variableNames": ["only-one"]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("must match the distribution dimension")))
    }

    @Test
    fun `should list available multivariate distributions`() {
        mockMvc.perform(get("/api/v1/distributions/multivariate"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].name").value("dirichlet"))
            .andExpect(jsonPath("$[0].description").isNotEmpty)
    }
}
