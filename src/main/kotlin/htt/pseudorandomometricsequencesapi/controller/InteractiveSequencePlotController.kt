package htt.pseudorandomometricsequencesapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

/**
 * Serves the self-contained interactive 3D viewer for univariate distributions.
 *
 * <p>The HTML page uses only the browser APIs (Canvas 2D, Fetch, Pointer and Wheel
 * events). It asks the public API for the requested sequence and computes the
 * histogram in the browser, so there is no server-side rendering cost and no
 * third-party JavaScript dependency.</p>
 */
@Tag(
    name = "Sequence Visualization",
    description = "Render a sequence as a static PNG or explore it in an interactive 3D viewer."
)
@RestController
@RequestMapping("/api/v1/random")
class InteractiveSequencePlotController {

    companion object {
        private val logger = LoggerFactory.getLogger(InteractiveSequencePlotController::class.java)
        private const val PAGE_LOCATION = "static/sequence-plot-3d.html"
    }

    private val page: String by lazy {
        ClassPathResource(PAGE_LOCATION).inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
            reader.readText()
        }
    }

    @Operation(
        summary = "Open the interactive 3D distribution viewer",
        description = "Returns a self-contained HTML page that renders the requested distribution as an " +
                "interactive 3D histogram. Drag with the mouse to rotate the camera, use the wheel to zoom, " +
                "and double-click to reset the view. The page accepts the same distribution, param1/param2/" +
                "param3, count, type, and seed query parameters as GET /api/v1/random/sequence. The optional " +
                "bins parameter controls the number of histogram bars (8-120). Open the URL directly in a browser.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "HTML page with the interactive viewer.",
                content = [Content(
                    mediaType = MediaType.TEXT_HTML_VALUE,
                    schema = Schema(type = "string")
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "A numeric query parameter could not be parsed.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            )
        ]
    )
    @GetMapping("/sequence/plot/3d", produces = [MediaType.TEXT_HTML_VALUE])
    fun getInteractivePlot(
        @Parameter(description = "Probability distribution to sample from.", example = "gaussian")
        @RequestParam(defaultValue = "gaussian") distribution: String,

        @Parameter(
            description = "First parameter of the distribution (meaning depends on the chosen distribution).",
            required = false,
            example = "0.0"
        )
        @RequestParam(required = false) param1: Double?,

        @Parameter(
            description = "Second parameter of the distribution (meaning depends on the chosen distribution).",
            required = false,
            example = "1.0"
        )
        @RequestParam(required = false) param2: Double?,

        @Parameter(
            description = "Third parameter of the distribution (meaning depends on the chosen distribution).",
            required = false,
            example = "1.0"
        )
        @RequestParam(required = false) param3: Double?,

        @Parameter(description = "Number of samples to generate for the view (1-200,000).", example = "20000")
        @RequestParam(defaultValue = "20000") count: Int,

        @Parameter(
            description = "Type of random number generator.",
            schema = Schema(allowableValues = ["general", "secure"]),
            example = "general"
        )
        @RequestParam(defaultValue = "general") type: String,

        @Parameter(description = "Seed used to reproduce the sequence.", required = false, example = "42")
        @RequestParam(required = false) seed: Long?,

        @Parameter(description = "Number of histogram bins rendered by the viewer (8-120).", example = "64")
        @RequestParam(defaultValue = "64") bins: Int
    ): ResponseEntity<String> {
        logger.info(
            "-> Interactive 3D viewer requested. Distribution: '{}', Params: [{}, {}, {}], Count: {}, " +
                    "Type: '{}', Seed: {}, Bins: {}",
            distribution, param1, param2, param3, count, type, seed, bins
        )

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("${MediaType.TEXT_HTML_VALUE};charset=UTF-8"))
            .cacheControl(CacheControl.noStore())
            .body(page)
    }
}
