package htt.pseudorandomometricsequencesapi.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {

    @GetMapping("/swagger")
    fun redirectToSwagger(): String {
        return "redirect:/swagger-ui.html"
    }

    @GetMapping("/")
    fun redirectToKDoc(): String {
        return "redirect:/doc/index.html"
    }
}