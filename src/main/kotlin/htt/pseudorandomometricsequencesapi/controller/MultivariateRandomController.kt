package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.MultivariateRandomService
import htt.pseudorandomometricsequencesapi.domain.multivariate.MultivariateSpec
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Pseudorandom Sequence Generator",
    description = "API for generating sequences of numbers from various probability distributions."
)
@RestController
@RequestMapping("/api/v1/random")
class MultivariateRandomController(private val multivariateRandomService: MultivariateRandomService) {

    companion object {
        private val logger = LoggerFactory.getLogger(MultivariateRandomController::class.java)
    }

    @Operation(
        summary = "Generate a Joint Multivariate Sequence",
        description = "Generates vector-valued samples from a joint distribution. Components within a sample may be " +
                "statistically dependent; for example, multivariate-normal uses the supplied covariance matrix to " +
                "produce correlated components. Use POST /api/v1/random/sequences instead when you only need " +
                "independent univariate sequences.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully generated the joint multivariate sequence.",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = MultivariateResponse::class)
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
    @PostMapping("/multivariate")
    fun getMultivariateSequence(@RequestBody request: MultivariateRequest): MultivariateResponse {
        logger.info(
            "-> Multivariate request received. Count: {}, Type: '{}', Dist: '{}', Seed: {}",
            request.count, request.type, request.distribution, request.seed
        )

        val result = multivariateRandomService.generate(
            count = request.count,
            type = request.type,
            spec = MultivariateSpec(
                distribution = request.distribution,
                mean = request.mean,
                covariance = request.covariance,
                degreesOfFreedom = request.degreesOfFreedom,
                concentration = request.concentration,
                trials = request.trials,
                probabilities = request.probabilities,
                variableNames = request.variableNames
            ),
            seed = request.seed
        )

        logger.info(
            "<- Multivariate request completed. Distribution: '{}', Samples: {}, Dimension: {}",
            result.distribution, result.sequence.size, result.variableNames.size
        )

        return MultivariateResponse(
            type = request.type,
            count = request.count,
            distribution = result.distribution,
            dimension = result.variableNames.size,
            seed = result.seed,
            variableNames = result.variableNames,
            sequence = result.sequence
        )
    }
}
