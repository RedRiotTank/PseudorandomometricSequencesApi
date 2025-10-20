package htt.pseudorandomometricsequencesapi.controller

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The generated pseudorandom number sequence and its parameters.")
data class RandomSequenceResponse(
    @get:Schema(
        description = "The type of random number generator used.",
        allowableValues = ["secure", "general"],
        example = "general"
    )
    val type: String,

    @get:Schema(description = "The number of samples requested.")
    val count: Int,

    @get:Schema(
        description = "The probability distribution used (e.g., 'uniform', 'gaussian').",
        allowableValues = ["uniform", "gaussian", "exponential", "gamma", "lognormal", "beta"],
        example = "uniform"
    )
    val distribution: String,

    @get:ArraySchema(
        arraySchema = Schema(description = "The list of generated pseudorandom numbers."),
        schema = Schema(implementation = Double::class)
    )
    val sequence: List<Double>
)