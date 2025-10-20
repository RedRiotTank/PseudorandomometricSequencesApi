package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.RandomService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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

    // --- COMPILE-TIME CONSTANT FOR DESCRIPTION ---
    companion object {
        // Using 'const val' and '\n' to ensure the string is a constant and the Markdown renders correctly.
        private const val DISTRIBUTION_DESCRIPTION =
            "The probability distribution to sample from.\n" +
                    "**Available Distributions:**\n" +
                    "* `uniform`: param1 = min (a), param2 = max (b)\n" +
                    "* `gaussian`: param1 = mean (μ), param2 = std. deviation (σ)\n" + // μ y σ
                    "* `exponential`: param1 = mean (or 1/λ)\n" + // λ
                    "* `gamma`: param1 = shape (k), param2 = scale (θ)\n" + // θ
                    "* `lognormal`: param1 = scale (μ), param2 = shape (σ)\n" + // μ y σ
                    "* `beta`: param1 = α, param2 = β" // α y β

    }
    // ---------------------------------------------


    @Operation(
        summary = "Generate a Pseudorandom Number Sequence",
        description = "Generates a list of numbers ('count') sampled from the specified probability 'distribution' using the chosen generator 'type' and its parameters.",
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
                description = "Invalid input parameters (e.g., negative 'count', invalid 'type', or parameter mismatch for 'distribution').",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Server error during sequence generation (e.g., internal library failure).",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    @GetMapping("/sequence")
    fun getRandomSequence(
        @Parameter(
            description = "The number of samples to generate. Must be a positive integer.",
            example = "100"
        )
        @RequestParam(defaultValue = "10") count: Int,

        @Parameter(
            description = "The type of random number generator to use.",
            schema = Schema(allowableValues = ["general", "secure"]),
            example = "general"
        )
        @RequestParam(defaultValue = "general") type: String,

        @Parameter(
            description = DISTRIBUTION_DESCRIPTION,
            schema = Schema(allowableValues = ["uniform","gaussian","exponential","gamma","lognormal","beta"]),
            example = "gaussian"
        )
        @RequestParam(defaultValue = "uniform") distribution: String,

        @Parameter(
            description = "The first parameter required by the specified distribution (e.g., lower bound, mean, or shape parameter).",
            required = false,
            example = "0.0"
        )
        @RequestParam(required = false) param1: Double?,

        @Parameter(
            description = "The second parameter required by the specified distribution (e.g., upper bound, standard deviation, or rate parameter).",
            required = false,
            example = "1.0"
        )
        @RequestParam(required = false) param2: Double?

    ): RandomSequenceResponse {
        val sequence = randomService.generateSequence(
            count,
            type,
            distribution,
            param1,
            param2
        )

        return RandomSequenceResponse(
            type = type,
            count = count,
            distribution = distribution,
            sequence = sequence
        )
    }
}