package htt.pseudorandomometricsequencesapi.controller

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request for generating a joint multivariate sequence.")
data class MultivariateRequest(
    @get:Schema(
        description = "Number of vector samples to generate. Must be positive.",
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
        description = "Seed used to reproduce the sequence.",
        example = "42"
    )
    val seed: Long? = null,

    @get:Schema(
        description = "Joint distribution to sample from: multivariate-normal, multivariate-t, dirichlet, multinomial.",
        example = "multivariate-normal"
    )
    val distribution: String,

    @get:Schema(
        description = "Mean vector for multivariate-normal or multivariate-t. Defaults to zeros.",
        example = "[0.0, 0.0]"
    )
    val mean: List<Double>? = null,

    @get:Schema(
        description = "Covariance (or scale) matrix for multivariate-normal or multivariate-t. " +
            "Must be symmetric positive definite.",
        example = "[[1.0, 0.8], [0.8, 1.0]]"
    )
    val covariance: List<List<Double>>? = null,

    @get:Schema(
        description = "Degrees of freedom for multivariate-t. Must be positive.",
        example = "5.0"
    )
    val degreesOfFreedom: Double? = null,

    @get:Schema(
        description = "Concentration vector for dirichlet. All values must be positive.",
        example = "[2.0, 5.0]"
    )
    val concentration: List<Double>? = null,

    @get:Schema(
        description = "Number of trials per sample for multinomial. Must be non-negative.",
        example = "10"
    )
    val trials: Int? = null,

    @get:Schema(
        description = "Category probabilities for multinomial. Must be non-negative and sum to 1.0.",
        example = "[0.2, 0.3, 0.5]"
    )
    val probabilities: List<Double>? = null,

    @get:Schema(
        description = "Optional component names. Defaults to x1, x2, ...; length must match the dimension.",
        example = "[\"height\", \"weight\"]"
    )
    val variableNames: List<String>? = null
)

@Schema(description = "A joint multivariate sequence. Each inner list is one vector observation.")
data class MultivariateResponse(
    @get:Schema(
        description = "The type of random number generator used.",
        allowableValues = ["secure", "general"],
        example = "general"
    )
    val type: String,

    @get:Schema(description = "The number of vector samples generated.")
    val count: Int,

    @get:Schema(description = "The joint distribution used.", example = "multivariate-normal")
    val distribution: String,

    @get:Schema(description = "The number of components in each sample.", example = "2")
    val dimension: Int,

    @get:Schema(description = "The seed used to generate the sequence.", example = "42")
    val seed: Long,

    @get:ArraySchema(
        arraySchema = Schema(description = "Names of the components, in vector order."),
        schema = Schema(implementation = String::class)
    )
    val variableNames: List<String>,

    @get:ArraySchema(
        arraySchema = Schema(
            description = "One vector observation per entry. Each inner array has 'dimension' values."
        ),
        schema = Schema(implementation = Double::class)
    )
    val sequence: List<List<Double>>
)
