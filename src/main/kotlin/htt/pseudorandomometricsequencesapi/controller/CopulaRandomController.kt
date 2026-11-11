package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.CopulaRandomService
import htt.pseudorandomometricsequencesapi.domain.copula.CopulaVariableSpec
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
class CopulaRandomController(private val copulaRandomService: CopulaRandomService) {

    companion object {
        private val logger = LoggerFactory.getLogger(CopulaRandomController::class.java)
    }

    @Operation(
        summary = "Generate Statistically Dependent Variables (Gaussian Copula)",
        description = "Generates one sequence per marginal variable, with dependence controlled by a " +
                "correlation matrix. Each marginal can use any registered univariate distribution. The copula " +
                "controls rank dependence: for non-normal marginals, the resulting Pearson correlation depends " +
                "on the marginal distributions, while the Gaussian-copula rank correlation is determined by the " +
                "matrix. Marginal inverse CDFs are approximated with empirical quantile tables.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully generated the dependent sequences.",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CopulaResponse::class)
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
    @PostMapping("/copula")
    fun getDependentSequences(@RequestBody request: CopulaRequest): CopulaResponse {
        logger.info(
            "-> Copula request received. Count: {}, Type: '{}', Variables: {}, Seed: {}, Quantile samples: {}",
            request.count, request.type, request.variables.size, request.seed, request.quantileSamples
        )

        val result = copulaRandomService.generate(
            count = request.count,
            type = request.type,
            variables = request.variables.map {
                CopulaVariableSpec(
                    name = it.name,
                    distribution = it.distribution,
                    param1 = it.param1,
                    param2 = it.param2,
                    param3 = it.param3
                )
            },
            correlation = request.correlation,
            seed = request.seed,
            quantileSamples = request.quantileSamples
        )

        logger.info(
            "<- Copula request completed. Variables: {}, Samples per variable: {}",
            result.variables.size, request.count
        )

        return CopulaResponse(
            type = request.type,
            count = request.count,
            copula = "gaussian",
            marginalMethod = "empirical-quantile",
            seed = result.seed,
            quantileSamples = result.quantileSamples,
            correlation = result.correlation,
            variables = result.variables.map {
                CopulaVariableResponse(
                    name = it.name,
                    distribution = it.distribution,
                    sequence = it.sequence
                )
            }
        )
    }
}
