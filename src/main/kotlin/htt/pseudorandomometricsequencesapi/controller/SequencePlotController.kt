package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.RandomService
import htt.pseudorandomometricsequencesapi.domain.visualization.SequencePlotInfo
import htt.pseudorandomometricsequencesapi.domain.visualization.SequencePlotOptions
import htt.pseudorandomometricsequencesapi.domain.visualization.SequencePlotRenderer
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Sequence Visualization",
    description = "Generate a ready-to-share 3D-style PNG image for any generated sequence."
)
@RestController
@RequestMapping("/api/v1/random")
class SequencePlotController(
    private val randomService: RandomService,
    private val sequencePlotRenderer: SequencePlotRenderer
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SequencePlotController::class.java)
    }

    @Operation(
        summary = "Render a 3D-style image of a Pseudorandom Sequence",
        description = "Generates the same sequence as GET /api/v1/random/sequence and renders it as a " +
                "dark-themed 3D density histogram. The response is a PNG image, so it can be opened directly " +
                "in a browser or saved with curl. Use the distribution and param1/param2/param3 query parameters " +
                "exactly as with the JSON endpoint.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "PNG image with the rendered sequence.",
                content = [Content(
                    mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = Schema(type = "string", format = "binary")
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid input parameters.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Server error while generating or rendering the sequence.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            )
        ]
    )
    @GetMapping("/sequence/plot", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getSequencePlot(
        @Parameter(
            description = "Number of samples to generate before rendering. More samples produce a smoother plot. " +
                    "Defaults to 20,000 for plotting; maximum 2,000,000.",
            example = "20000"
        )
        @RequestParam(defaultValue = "20000") count: Int,

        @Parameter(
            description = "Type of random number generator.",
            schema = Schema(allowableValues = ["general", "secure"]),
            example = "general"
        )
        @RequestParam(defaultValue = "general") type: String,

        @Parameter(description = "Probability distribution to sample from.", example = "gaussian")
        @RequestParam(defaultValue = "uniform") distribution: String,

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

        @Parameter(description = "Seed to reproduce both the sequence and the image.", required = false, example = "42")
        @RequestParam(required = false) seed: Long?,

        @Parameter(description = "Image width in pixels (400–3200).", example = "1400")
        @RequestParam(defaultValue = "1400") width: Int,

        @Parameter(description = "Image height in pixels (300–3200).", example = "1000")
        @RequestParam(defaultValue = "1000") height: Int,

        @Parameter(description = "Number of histogram bins used to estimate the density (8–200).", example = "64")
        @RequestParam(defaultValue = "64") bins: Int
    ): ResponseEntity<ByteArray> {
        logger.info(
            "-> Plot request received. Count: {}, Type: '{}', Dist: '{}', Params: [{}, {}, {}], Seed: {}, " +
                    "Size: {}x{}, Bins: {}",
            count, type, distribution, param1, param2, param3, seed, width, height, bins
        )

        val result = randomService.generateSequence(
            count = count,
            type = type,
            distribution = distribution,
            param1 = param1,
            param2 = param2,
            param3 = param3,
            seed = seed
        )

        val image = sequencePlotRenderer.render(
            sequence = result.sequence,
            info = SequencePlotInfo(
                distribution = distribution,
                seed = result.seed,
                param1 = param1,
                param2 = param2,
                param3 = param3
            ),
            options = SequencePlotOptions(width = width, height = height, bins = bins)
        )

        logger.info(
            "<- Plot request completed. Distribution: '{}', Samples: {}, PNG bytes: {}",
            distribution, result.sequence.size, image.size
        )

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.noStore())
            .body(image)
    }

    @Operation(
        summary = "Render an existing sequence",
        description = "Renders any sequence supplied in the request body as the same 3D-style PNG image. " +
                "This is useful for visualizing individual variables returned by /api/v1/random/sequences, " +
                "/api/v1/random/copula, or any other source.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "PNG image with the rendered sequence.",
                content = [Content(
                    mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = Schema(type = "string", format = "binary")
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid input parameters.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Server error while rendering the sequence.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            )
        ]
    )
    @PostMapping(
        "/sequence/plot",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.IMAGE_PNG_VALUE]
    )
    fun renderSequencePlot(@RequestBody request: SequencePlotRequest): ResponseEntity<ByteArray> {
        require(request.sequence.isNotEmpty()) { "sequence must not be empty" }
        require(request.sequence.size <= RandomService.MAX_SAMPLES_PER_REQUEST) {
            "sequence cannot contain more than ${RandomService.MAX_SAMPLES_PER_REQUEST} values"
        }

        logger.info(
            "-> Custom plot request received. Samples: {}, Distribution: '{}', Seed: {}, Size: {}x{}, Bins: {}",
            request.sequence.size, request.distribution, request.seed, request.width, request.height, request.bins
        )

        val image = sequencePlotRenderer.render(
            sequence = request.sequence,
            info = SequencePlotInfo(
                distribution = request.distribution ?: "sequence",
                seed = request.seed
            ),
            options = SequencePlotOptions(width = request.width, height = request.height, bins = request.bins)
        )

        logger.info("<- Custom plot request completed. PNG bytes: {}", image.size)

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.noStore())
            .body(image)
    }
}
