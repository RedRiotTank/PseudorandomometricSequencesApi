package htt.pseudorandomometricsequencesapi.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "api.limits.max-request-body-bytes=64",
        "api.limits.max-generation-millis=1",
        "api.limits.max-requests-per-minute=100000",
        "api.limits.max-requests-per-minute-per-ip=100000",
        "api.limits.max-concurrent-values=10"
    ]
)
@AutoConfigureMockMvc
class ApiLimitsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should reject requests whose projected generation time is over budget`() {
        mockMvc.perform(
            get("/api/v1/random/sequence")
                .param("count", "2000000")
                .param("distribution", "f-distribution")
                .param("seed", "42")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("budget")))
    }

    @Test
    fun `should reject oversized request bodies`() {
        val body = """{"count":1,"variables":[{"distribution":"uniform"}]}"""
            .padEnd(100, ' ')

        mockMvc.perform(
            post("/api/v1/random/sequences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isPayloadTooLarge)
    }
}
