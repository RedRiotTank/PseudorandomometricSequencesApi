package htt.pseudorandomometricsequencesapi.controller

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class InteractiveSequencePlotControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should serve the interactive 3D viewer as HTML`() {
        mockMvc.perform(get("/api/v1/random/sequence/plot/3d"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("Interactive Distribution Viewer")))
            .andExpect(content().string(containsString("id=\"chart\"")))
            .andExpect(content().string(containsString("/api/v1/random/sequence")))
    }

    @Test
    fun `should accept the same sequence parameters as the plot endpoint`() {
        mockMvc.perform(
            get("/api/v1/random/sequence/plot/3d")
                .param("distribution", "gaussian")
                .param("param1", "100")
                .param("param2", "15")
                .param("count", "50000")
                .param("seed", "42")
                .param("bins", "48")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }

    @Test
    fun `should reject a non-numeric distribution parameter`() {
        mockMvc.perform(
            get("/api/v1/random/sequence/plot/3d")
                .param("distribution", "gaussian")
                .param("param1", "not-a-number")
        )
            .andExpect(status().isBadRequest)
    }
}
