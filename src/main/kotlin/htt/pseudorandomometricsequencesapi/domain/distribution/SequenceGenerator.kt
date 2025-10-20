package htt.pseudorandomometricsequencesapi.domain.distribution

fun interface SequenceGenerator {
    fun sample(): Double
}