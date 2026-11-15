package htt.pseudorandomometricsequencesapi.security

import com.fasterxml.jackson.databind.ObjectMapper
import htt.pseudorandomometricsequencesapi.config.ApiLimitsProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.io.ByteArrayInputStream

class ApiProtectionFilterTest {

    private val objectMapper = ObjectMapper()

    private fun limits(
        perIp: Int = 100,
        global: Int = 10_000,
        maxBody: Long = 1_048_576
    ) = ApiLimitsProperties().apply {
        maxRequestsPerMinutePerIp = perIp
        maxRequestsPerMinute = global
        maxRequestBodyBytes = maxBody
    }

    private fun request(
        path: String = "/api/v1/random/sequence",
        method: String = "GET",
        ip: String = "203.0.113.10"
    ) = MockHttpServletRequest(method, path).apply {
        remoteAddr = ip
    }

    private fun chain(): FilterChain = FilterChain { _, _ -> }

    @Test
    fun `should allow generation requests under the per-IP limit`() {
        val filter = ApiProtectionFilter(limits(perIp = 2), objectMapper)

        repeat(2) {
            val response = MockHttpServletResponse()
            filter.doFilter(request(), response, chain())
            assertEquals(200, response.status)
        }

        val rejected = MockHttpServletResponse()
        filter.doFilter(request(), rejected, chain())
        assertEquals(429, rejected.status)
    }

    @Test
    fun `should isolate rate limits per client IP`() {
        val filter = ApiProtectionFilter(limits(perIp = 1), objectMapper)

        val first = MockHttpServletResponse()
        filter.doFilter(request(ip = "203.0.113.1"), first, chain())
        assertEquals(200, first.status)

        val second = MockHttpServletResponse()
        filter.doFilter(request(ip = "203.0.113.2"), second, chain())
        assertEquals(200, second.status)
    }

    @Test
    fun `should reject requests with an oversized declared body`() {
        val filter = ApiProtectionFilter(limits(maxBody = 10), objectMapper)
        val request = request(method = "POST", path = "/api/v1/random/sequences")
        request.setContent(ByteArray(20))

        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain())

        assertEquals(413, response.status)
    }

    @Test
    fun `should reject chunked bodies that grow past the limit`() {
        val filter = ApiProtectionFilter(limits(maxBody = 10), objectMapper)
        val request = object : MockHttpServletRequest("POST", "/api/v1/random/sequences") {
            override fun getContentLengthLong(): Long = -1

            override fun getInputStream(): ServletInputStream = object : ServletInputStream() {
                private val delegate = ByteArrayInputStream(ByteArray(20))

                override fun read(): Int = delegate.read()

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    delegate.read(buffer, offset, length)

                override fun isFinished(): Boolean = delegate.available() == 0

                override fun isReady(): Boolean = true

                override fun setReadListener(readListener: ReadListener) = Unit
            }
        }.apply { remoteAddr = "203.0.113.10" }

        val response = MockHttpServletResponse()
        filter.doFilter(
            request,
            response,
            FilterChain { req, _ ->
                val stream = (req as HttpServletRequest).inputStream
                stream.readAllBytes()
            }
        )

        assertEquals(413, response.status)
    }

    @Test
    fun `should not rate limit documentation endpoints`() {
        val filter = ApiProtectionFilter(limits(perIp = 1), objectMapper)

        repeat(5) {
            val response = MockHttpServletResponse()
            filter.doFilter(request(path = "/api/v1/distributions"), response, chain())
            assertEquals(200, response.status)
        }
    }
}
