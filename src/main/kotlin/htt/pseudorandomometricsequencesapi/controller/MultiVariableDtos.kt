package htt.pseudorandomometricsequencesapi.controller

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request for generating multiple variables in a single call.")
data class MultiSequenceRequest(
    @get:Schema(
        description = "Number of samples to generate for each variable. Must be positive.",
        example = "100"
    )
    val count: Int = 10,

    @get:Schema(
        description = "The base generator type.",
        allowableValues = ["general", "secure"],
        example = "general"
    )
    val type: String = "general",

    @get:Schema(
        description = "Optional request-level seed. Per-variable seeds are derived from it deterministically.",
        example = "42"
    )
    val seed: Long? = null,

    @get:Schema(
        description = "The variables to generate. Each variable can use a different distribution and parameters.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val variables: List<VariableRequest> = emptyList()
)

@Schema(description = "Definition of one variable to generate.")
data class VariableRequest(
    @get:Schema(
        description = "Optional unique name. Defaults to 'variable1', 'variable2', ... in request order.",
        example = "temperature"
    )
    val name: String? = null,

    @get:Schema(
        description = "Probability distribution to sample from. See /api/v1/distributions for the full list.",
        example = "gaussian"
    )
    val distribution: String,

    @get:Schema(
        description = "First parameter of the distribution (meaning depends on the distribution).",
        example = "0.0"
    )
    val param1: Double? = null,

    @get:Schema(
        description = "Second parameter of the distribution (meaning depends on the distribution).",
        example = "1.0"
    )
    val param2: Double? = null,

    @get:Schema(
        description = "Third parameter of the distribution (meaning depends on the distribution).",
        example = "1.0"
    )
    val param3: Double? = null,

    @get:Schema(
        description = "Optional seed for this variable. Overrides the seed derived from the request-level seed.",
        example = "123"
    )
    val seed: Long? = null
)

@Schema(description = "The generated sequences for all requested variables.")
data class MultiSequenceResponse(
    @get:Schema(
        description = "The type of random number generator used.",
        allowableValues = ["secure", "general"],
        example = "general"
    )
    val type: String,

    @get:Schema(description = "The number of samples generated for each variable.")
    val count: Int,

    @get:Schema(
        description = "The request-level seed used to derive the per-variable seeds.",
        example = "42"
    )
    val seed: Long,

    @get:ArraySchema(
        arraySchema = Schema(description = "One entry per requested variable, in request order."),
        schema = Schema(implementation = VariableSequenceResponse::class)
    )
    val variables: List<VariableSequenceResponse>
)

@Schema(description = "The generated sequence for one variable.")
data class VariableSequenceResponse(
    @get:Schema(description = "The resolved variable name.", example = "temperature")
    val name: String,

    @get:Schema(description = "The probability distribution used.", example = "gaussian")
    val distribution: String,

    @get:Schema(description = "The seed used for this variable.", example = "42")
    val seed: Long,

    @get:ArraySchema(
        arraySchema = Schema(description = "The list of generated pseudorandom numbers."),
        schema = Schema(implementation = Double::class)
    )
    val sequence: List<Double>
)
