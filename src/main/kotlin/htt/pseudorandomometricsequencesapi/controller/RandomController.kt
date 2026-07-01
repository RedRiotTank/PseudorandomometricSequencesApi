package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.RandomService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Pseudorandom Sequence Generator",
    description = "API for generating sequences of numbers from various probability distributions."
)
@RestController
@RequestMapping("/api/v1/random")
class RandomController(private val randomService: RandomService) {

    companion object {
        private val logger = LoggerFactory.getLogger(RandomController::class.java)
    }

    @Operation(
        summary = "Generate a Pseudorandom Number Sequence",
        description = "Generates a list of numbers sampled from the specified probability distribution. " +
                "See the 'distribution' parameter for the full list of available distributions and their parameters.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully generated the pseudorandom sequence.",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = RandomSequenceResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid input parameters.",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Server error during sequence generation.",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @GetMapping("/sequence")
    fun getRandomSequence(
        @Parameter(description = "Number of samples to generate. Must be a positive integer.", example = "100")
        @RequestParam(defaultValue = "10") count: Int,

        @Parameter(
            description = "Type of random number generator.",
            schema = Schema(allowableValues = ["general", "secure"]),
            example = "general"
        )
        @RequestParam(defaultValue = "general") type: String,

        // Description and allowable values are populated dynamically by DistributionSwaggerCustomizer
        @Parameter(description = "Probability distribution to sample from.", example = "gaussian")
        @RequestParam(defaultValue = "uniform") distribution: String,

        @Parameter(description = "First parameter of the distribution (meaning depends on the chosen distribution).", required = false, example = "0.0")
        @RequestParam(required = false) param1: Double?,

        @Parameter(description = "Second parameter of the distribution (meaning depends on the chosen distribution).", required = false, example = "1.0")
        @RequestParam(required = false) param2: Double?,

        @Parameter(description = "Third parameter of the distribution (meaning depends on the chosen distribution).", required = false, example = "1.0")
        @RequestParam(required = false) param3: Double?,

        @Parameter(description = "Seed to reproduce the results.", required = false, example = "42")
        @RequestParam(required = false) seed: Long?

    ): RandomSequenceResponse {

        logger.info("-> Sequence request received. Count: {}, Type: '{}', Dist: '{}', Params: [{}, {}, {}], Seed: {}",
            count, type, distribution, param1, param2, param3, seed)

        val result = randomService.generateSequence(count, type, distribution, param1, param2, param3, seed)

        logger.info("<- Request completed. Count: {}, Distribution: '{}'. First: {}",
            result.sequence.size, distribution, result.sequence.firstOrNull())

        return RandomSequenceResponse(
            type = type,
            count = count,
            distribution = distribution,
            seed = result.seed,
            sequence = result.sequence
        )
    }
}
