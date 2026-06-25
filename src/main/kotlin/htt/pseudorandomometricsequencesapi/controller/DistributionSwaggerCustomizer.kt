package htt.pseudorandomometricsequencesapi.controller

import htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

/**
 * Builds the Swagger description and enum values for the `distribution` parameter automatically
 * from all registered {@link DistributionFactory} components.
 *
 * <p>Adding a new distribution only requires creating a new {@code @Component} that implements
 * {@code DistributionFactory}. This customizer picks it up at startup and reflects it in the
 * Swagger UI without any manual edits.</p>
 */
@Configuration
class DistributionSwaggerCustomizer(private val factories: List<DistributionFactory>) {

    @Bean
    fun distributionParameterCustomizer(): OperationCustomizer {
        val sorted = factories.sortedBy { it.name }
        val description = buildDescription(sorted)
        val names: List<String> = sorted.map { it.name }

        return OperationCustomizer { operation: Operation, _: HandlerMethod ->
            operation.parameters
                ?.find { it.name == "distribution" }
                ?.let { param ->
                    param.description = description
                    param.schema = StringSchema().apply {
                        enum = names.toMutableList()
                        setDefault("uniform")
                    }
                }
            operation
        }
    }

    private fun buildDescription(sorted: List<DistributionFactory>): String {
        val lines = sorted.joinToString("\n") { "* `${it.name}`: ${it.description}" }
        return "The probability distribution to sample from.\n\n" +
               "**Available distributions (${sorted.size}):**\n$lines"
    }
}
