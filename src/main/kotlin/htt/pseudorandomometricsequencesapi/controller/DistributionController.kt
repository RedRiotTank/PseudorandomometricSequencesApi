package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Distribution Metadata",
    description = "API for retrieving metadata about supported probability distributions."
)
@RestController
@RequestMapping("/api/v1/distributions")
class DistributionController(private val factories: List<DistributionFactory>) {

    @Operation(
        summary = "Get all available distributions",
        description = "Returns a list of all available probability distributions with their parameters and descriptions."
    )
    @GetMapping
    fun getDistributions(): List<DistributionInfo> {
        return factories.map {
            DistributionInfo(
                name = it.name,
                description = it.description
            )
        }.sortedBy { it.name }
    }
}

data class DistributionInfo(
    val name: String,
    val description: String
)
