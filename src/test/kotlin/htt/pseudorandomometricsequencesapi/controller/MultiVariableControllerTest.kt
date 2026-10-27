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
class MultiVariableControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun performMulti(request: String) =
        mockMvc.perform(
            post("/api/v1/random/sequences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )

    @Test
    fun `should generate multiple variables with their own distributions`() {
        performMulti(
            """
            {
              "count": 4,
              "type": "general",
              "seed": 42,
              "variables": [
                {"name": "x", "distribution": "uniform", "param1": 0, "param2": 1},
                {"name": "y", "distribution": "gaussian", "param1": 0, "param2": 1}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.type").value("general"))
            .andExpect(jsonPath("$.count").value(4))
            .andExpect(jsonPath("$.seed").value(42))
            .andExpect(jsonPath("$.variables.length()").value(2))
            .andExpect(jsonPath("$.variables[0].name").value("x"))
            .andExpect(jsonPath("$.variables[0].distribution").value("uniform"))
            .andExpect(jsonPath("$.variables[0].sequence.length()").value(4))
            .andExpect(jsonPath("$.variables[1].name").value("y"))
            .andExpect(jsonPath("$.variables[1].distribution").value("gaussian"))
            .andExpect(jsonPath("$.variables[1].sequence.length()").value(4))
    }

    @Test
    fun `should assign default variable names when omitted`() {
        performMulti(
            """
            {
              "count": 2,
              "variables": [
                {"distribution": "uniform"},
                {"distribution": "gaussian"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("general"))
            .andExpect(jsonPath("$.variables[0].name").value("variable1"))
            .andExpect(jsonPath("$.variables[1].name").value("variable2"))
    }

    @Test
    fun `should honor a variable-specific seed`() {
        performMulti(
            """
            {
              "count": 3,
              "seed": 100,
              "variables": [
                {"name": "x", "distribution": "uniform", "seed": 7}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.variables[0].seed").value(7))
            .andExpect(jsonPath("$.variables[0].sequence.length()").value(3))
    }

    @Test
    fun `should return identical responses for the same request seed`() {
        val request = """
            {
              "count": 10,
              "seed": 24680,
              "variables": [
                {"name": "x", "distribution": "uniform"},
                {"name": "y", "distribution": "gaussian"}
              ]
            }
            """.trimIndent()

        val first = performMulti(request).andExpect(status().isOk).andReturn()
            .response.contentAsString
        val second = performMulti(request).andExpect(status().isOk).andReturn()
            .response.contentAsString

        val firstJson = objectMapper.readTree(first)
        val secondJson = objectMapper.readTree(second)

        assertEquals(firstJson, secondJson)
        assertEquals(firstJson["variables"], secondJson["variables"])
    }

    @Test
    fun `should reject an empty variable list`() {
        performMulti("""{"count": 5, "variables": []}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("At least one variable must be specified."))
    }

    @Test
    fun `should reject a request without variables`() {
        performMulti("""{"count": 5}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("At least one variable must be specified."))
    }

    @Test
    fun `should reject duplicate variable names`() {
        performMulti(
            """
            {
              "count": 2,
              "variables": [
                {"name": "same", "distribution": "uniform"},
                {"name": "same", "distribution": "gaussian"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Variable names must be unique."))
    }

    @Test
    fun `should reject an invalid distribution`() {
        performMulti(
            """
            {
              "count": 1,
              "variables": [
                {"name": "x", "distribution": "does-not-exist"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Invalid Distribution")))
    }

    @Test
    fun `should reject a malformed request body`() {
        performMulti("""{"count": 1, "variables": [}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid or missing request body."))
    }

    @Test
    fun `should reject a variable without a distribution`() {
        performMulti(
            """
            {
              "count": 1,
              "variables": [
                {"name": "x"}
              ]
            }
            """.trimIndent()
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid or missing request body."))
    }

    @Test
    fun `should keep the original single-sequence endpoint working`() {
        mockMvc.perform(
            get("/api/v1/random/sequence")
                .param("count", "3")
                .param("distribution", "gaussian")
                .param("seed", "42")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("general"))
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.distribution").value("gaussian"))
            .andExpect(jsonPath("$.seed").value(42))
            .andExpect(jsonPath("$.sequence.length()").value(3))
    }
}
