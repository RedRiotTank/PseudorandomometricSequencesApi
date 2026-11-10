package htt.pseudorandomometricsequencesapi.controller

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Request for rendering an existing sequence as a 3D-style PNG image. " +
        "Use it to visualize a variable returned by /api/v1/random/sequences or /api/v1/random/copula."
)
data class SequencePlotRequest(
    @get:ArraySchema(
        arraySchema = Schema(
            description = "Values to render. Accepts any sequence produced by the API.",
            requiredMode = Schema.RequiredMode.REQUIRED
        ),
        schema = Schema(implementation = Double::class)
    )
    val sequence: List<Double> = emptyList(),

    @get:Schema(
        description = "Optional label used as the image title, for example the source distribution.",
        example = "gaussian"
    )
    val distribution: String? = null,

    @get:Schema(
        description = "Optional seed shown in the subtitle, useful to keep the source request documented.",
        example = "42"
    )
    val seed: Long? = null,

    @get:Schema(description = "Image width in pixels (400–3200).", example = "1400")
    val width: Int = 1400,

    @get:Schema(description = "Image height in pixels (300–3200).", example = "1000")
    val height: Int = 1000,

    @get:Schema(description = "Number of histogram bins used to estimate the density (8–200).", example = "64")
    val bins: Int = 64
)
