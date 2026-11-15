package htt.pseudorandomometricsequencesapi.controller

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
class SequencePlotControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should render a PNG for a Gaussian sequence`() {
        val response = mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "gaussian")
                .param("param1", "100")
                .param("param2", "15")
                .param("count", "2000")
                .param("seed", "42")
                .param("width", "800")
                .param("height", "600")
                .param("bins", "32")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
            .andReturn()

        val bytes = response.response.contentAsByteArray
        assertTrue(bytes.size > 1_000, "Expected a non-trivial PNG image")
        assertArrayEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            bytes.copyOfRange(0, 8)
        )

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertEquals(800, image.width)
        assertEquals(600, image.height)
    }

    @Test
    fun `should use a plotting-friendly default sample count`() {
        val response = mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "uniform")
                .param("seed", "7")
                .param("width", "640")
                .param("height", "480")
        )
            .andExpect(status().isOk)
            .andReturn()

        val image = ImageIO.read(ByteArrayInputStream(response.response.contentAsByteArray))
        assertEquals(640, image.width)
        assertEquals(480, image.height)
    }

    @Test
    fun `should render identical images for the same seed`() {
        fun request() = mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "exponential")
                .param("param1", "2.5")
                .param("count", "1500")
                .param("seed", "2026")
                .param("width", "700")
                .param("height", "500")
                .param("bins", "48")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray

        assertArrayEquals(request(), request())
    }

    @Test
    fun `should reject an out-of-range bin count`() {
        mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "gaussian")
                .param("bins", "5")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("bins must be between")))
    }

    @Test
    fun `should reject an out-of-range image size`() {
        mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "gaussian")
                .param("width", "100")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("width must be between")))
    }

    @Test
    fun `should render discrete distributions`() {
        val response = mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "poisson")
                .param("param1", "3")
                .param("count", "1000")
                .param("seed", "11")
                .param("width", "640")
                .param("height", "480")
                .param("bins", "32")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
            .andReturn()

        val image = ImageIO.read(ByteArrayInputStream(response.response.contentAsByteArray))
        assertEquals(640, image.width)
        assertEquals(480, image.height)
    }

    @Test
    fun `should reject an invalid distribution`() {
        mockMvc.perform(
            get("/api/v1/random/sequence/plot")
                .param("distribution", "not-a-distribution")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Invalid Distribution")))
    }

    @Test
    fun `should render an existing sequence from a JSON body`() {
        val sequence = (1..500).map { it * 0.5 }
        val body = """
            {
              "sequence": [${sequence.joinToString(",")}],
              "distribution": "custom",
              "seed": 99,
              "width": 720,
              "height": 540,
              "bins": 24
            }
        """.trimIndent()

        val response = mockMvc.perform(
            post("/api/v1/random/sequence/plot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
            .andReturn()

        val image = ImageIO.read(ByteArrayInputStream(response.response.contentAsByteArray))
        assertEquals(720, image.width)
        assertEquals(540, image.height)
    }

    @Test
    fun `should reject an empty sequence body`() {
        mockMvc.perform(
            post("/api/v1/random/sequence/plot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sequence": [], "distribution": "empty"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("sequence must not be empty")))
    }
}
