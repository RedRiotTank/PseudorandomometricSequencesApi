package htt.pseudorandomometricsequencesapi


import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/random")
class RandomController(private val randomService: RandomService) {

    @GetMapping("/sequence")
    fun getRandomSequence(
        @RequestParam(defaultValue = "10") count: Int,
        @RequestParam(defaultValue = "general") type: String,
        @RequestParam(defaultValue = "uniform") distribution: String
    ): RandomSequenceResponse {

        val sequence = randomService.generateSequence(count, type, distribution)

        return RandomSequenceResponse(
            type = type,
            count = count,
            distribution = distribution,
            sequence = sequence
        )
    }
}