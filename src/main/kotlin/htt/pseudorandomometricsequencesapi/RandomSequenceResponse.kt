package htt.pseudorandomometricsequencesapi

data class RandomSequenceResponse(
    val type: String,
    val count: Int,
    val distribution: String,
    val sequence: List<Double>
)