package htt.pseudorandomometricsequencesapi.controller

/**
 * Structured metadata for one positional parameter of a univariate distribution.
 *
 * @property position API parameter position (1, 2, or 3).
 * @property label Human-readable parameter name, such as "mean" or "degrees of freedom".
 * @property symbol Mathematical symbol shown by the interactive viewer, when one is available.
 * @property defaultValue Default value used by the generator when the parameter is omitted.
 * @property integer Whether the parameter is parsed as a whole number.
 */
data class DistributionParameterInfo(
    val position: Int,
    val label: String,
    val symbol: String?,
    val defaultValue: Double?,
    val integer: Boolean
)

/**
 * Extracts [DistributionParameterInfo] from the one-line descriptions exposed by
 * the existing [htt.pseudorandomometricsequencesapi.domain.distribution.DistributionFactory]
 * implementations.
 *
 * <p>The descriptions follow a stable convention: `paramN = label symbol (def. value)`.
 * Parsing them here keeps the catalog as the single source of truth without adding
 * parameter metadata to all 81 factories.</p>
 */
object DistributionParameterParser {

    private val parameterPattern = Regex(
        """param(\d)\s*=\s*(.*?)(?=,\s*param\d\s*=|$)""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val defaultPattern = Regex(
        """\(def\.\s*([-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?)"""
    )
    private val tagPattern = Regex("""\[(?:integer|radians)]""")
    private val constraintPattern = Regex("""\s+(?:in\s+.*|[<>=≥≤]\s*.*)$""")
    private val whitespacePattern = Regex("""\s+""")
    private val symbolPattern = Regex("""^[\p{L}][\p{L}\p{N}₀-₉⁰¹²³⁴⁵⁶⁷⁸⁹/]*$""")

    /**
     * Returns one entry per `paramN` occurrence, sorted by position.
     */
    fun parse(description: String): List<DistributionParameterInfo> =
        parameterPattern.findAll(description).map { match ->
            val position = match.groupValues[1].toInt()
            val body = match.groupValues[2].trim()
            val defaultValue = defaultPattern.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            val integer = body.contains("[integer]", ignoreCase = true)
            val head = tagPattern.replace(body.substringBefore("(def."), "").trim()
            val (label, symbol) = parseLabelAndSymbol(head)

            DistributionParameterInfo(
                position = position,
                label = label,
                symbol = symbol,
                defaultValue = defaultValue,
                integer = integer
            )
        }.sortedBy { it.position }.toList()

    private fun parseLabelAndSymbol(head: String): Pair<String, String?> {
        var text = clean(head)
        var symbol = findSymbol(text)

        if (symbol == null) {
            val stripped = clean(constraintPattern.replace(text, ""))
            if (stripped.isNotEmpty() && stripped != text) {
                text = stripped
                symbol = findSymbol(text)
            }
        }

        val label = if (symbol == null) {
            text
        } else {
            clean(text.removeSuffix(symbol)).trim(' ', '-', '/', '=')
        }

        return if (label.isBlank()) {
            (symbol ?: text) to symbol
        } else {
            label to symbol
        }
    }

    private fun findSymbol(text: String): String? {
        val candidate = text.substringAfterLast(' ').trim()
        if (candidate.length !in 1..4) return null
        if (!candidate.matches(symbolPattern)) return null
        return candidate
    }

    private fun clean(text: String): String =
        whitespacePattern.replace(text, " ").trim()
}
