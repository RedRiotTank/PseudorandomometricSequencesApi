package htt.pseudorandomometricsequencesapi.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "api.limits.max-requests-per-minute=100000",
        "api.limits.max-requests-per-minute-per-ip=2",
        "api.limits.max-concurrent-values=10",
        "api.limits.max-generation-millis=5000"
    ]
)
@AutoConfigureMockMvc
class RateLimitControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 429 with Retry-After after the per-IP burst is exhausted`() {
        repeat(2) {
            mockMvc.perform(
                get("/api/v1/random/sequence")
                    .param("count", "1")
                    .param("distribution", "uniform")
            ).andExpect(status().isOk)
        }

        mockMvc.perform(
            get("/api/v1/random/sequence")
                .param("count", "1")
                .param("distribution", "uniform")
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
    }
}
