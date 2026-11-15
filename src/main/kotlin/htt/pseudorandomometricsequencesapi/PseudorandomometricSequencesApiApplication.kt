package htt.pseudorandomometricsequencesapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PseudorandomometricSequencesApiApplication

fun main(args: Array<String>) {
    runApplication<PseudorandomometricSequencesApiApplication>(*args)
}
