package htt.pseudorandomometricsequencesapi.domain.visualization

import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.Random
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Size and resolution of a rendered sequence plot.
 *
 * @property width Image width in pixels.
 * @property height Image height in pixels.
 * @property bins Number of histogram bins used to estimate the density.
 */
data class SequencePlotOptions(
    val width: Int = 1400,
    val height: Int = 1000,
    val bins: Int = 64
)

/**
 * Metadata rendered alongside a generated sequence.
 *
 * @property distribution Distribution name, as accepted by the API.
 * @property count Number of samples in the sequence.
 * @property seed Seed used to generate the sequence.
 * @property param1 First distribution parameter, when provided.
 * @property param2 Second distribution parameter, when provided.
 * @property param3 Third distribution parameter, when provided.
 */
data class SequencePlotInfo(
    val distribution: String,
    val seed: Long? = null,
    val param1: Double? = null,
    val param2: Double? = null,
    val param3: Double? = null
)

/**
 * Renders a univariate sequence as a dark-themed, 3D-looking density histogram.
 *
 * <p>The renderer only depends on the JDK's Java2D API, so it works in the same
 * headless container as the API and does not require Python or any plotting
 * library. Each histogram bin is drawn as a shaded cuboid; the sample cloud,
 * density footprint and axes are projected with a fixed isometric camera.</p>
 */
@Service
class SequencePlotRenderer {

    companion object {
        const val MIN_WIDTH = 400
        const val MAX_WIDTH = 3200
        const val MIN_HEIGHT = 300
        const val MAX_HEIGHT = 3200
        const val MIN_BINS = 8
        const val MAX_BINS = 200
        private const val MAX_DISCRETE_SUPPORT = 80
    }

    /**
     * Renders [sequence] to a PNG image.
     *
     * @throws IllegalArgumentException when the sequence is empty or the options
     * are outside the supported ranges.
     */
    fun render(
        sequence: List<Double>,
        info: SequencePlotInfo,
        options: SequencePlotOptions = SequencePlotOptions()
    ): ByteArray {
        require(options.width in MIN_WIDTH..MAX_WIDTH) {
            "width must be between $MIN_WIDTH and $MAX_WIDTH"
        }
        require(options.height in MIN_HEIGHT..MAX_HEIGHT) {
            "height must be between $MIN_HEIGHT and $MAX_HEIGHT"
        }
        require(options.bins in MIN_BINS..MAX_BINS) {
            "bins must be between $MIN_BINS and $MAX_BINS"
        }

        val prepared = prepare(sequence, options.bins)

        val image = BufferedImage(options.width, options.height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            configure(graphics)
            drawBackground(graphics, options.width, options.height)
            Scene(graphics, options.width, options.height, prepared, info).draw()
        } finally {
            graphics.dispose()
        }

        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    private fun configure(graphics: Graphics2D) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

    private fun drawBackground(graphics: Graphics2D, width: Int, height: Int) {
        graphics.paint = GradientPaint(
            0f,
            0f,
            Color(7, 11, 20),
            0f,
            height.toFloat(),
            Color(13, 21, 38)
        )
        graphics.fillRect(0, 0, width, height)

        graphics.color = Color(30, 64, 175, 26)
        graphics.fillOval(
            (-width * 0.20).toInt(),
            (-height * 0.45).toInt(),
            (width * 0.95).toInt(),
            (height * 0.95).toInt()
        )
        graphics.color = Color(45, 212, 191, 16)
        graphics.fillOval(
            (width * 0.55).toInt(),
            (-height * 0.30).toInt(),
            (width * 0.70).toInt(),
            (height * 0.80).toInt()
        )
    }

    private fun prepare(sequence: List<Double>, bins: Int): PreparedSequence {
        val values = sequence.asSequence().filter { it.isFinite() }.toList()
        require(values.isNotEmpty()) { "Cannot plot a sequence without finite values." }

        val sorted = values.toDoubleArray()
        sorted.sort()

        var lo: Double
        var hi: Double
        var density: DoubleArray
        var maxDensity: Double
        var isDiscrete = false
        var discreteValues: IntArray? = null

        val discreteRange = detectDiscreteRange(sorted)
        if (discreteRange != null) {
            val (minValue, maxValue) = discreteRange
            val support = maxValue - minValue + 1
            lo = minValue - 0.5
            hi = maxValue + 0.5

            val counts = IntArray(support)
            var inside = 0
            for (value in sorted) {
                val index = value.roundToInt() - minValue
                if (index in 0 until support) {
                    counts[index]++
                    inside++
                }
            }

            val total = max(inside, 1)
            density = DoubleArray(support) { counts[it].toDouble() / total }
            maxDensity = density.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
            isDiscrete = true
            discreteValues = IntArray(support) { minValue + it }
        } else {
            lo = quantile(sorted, 0.005)
            hi = quantile(sorted, 0.995)
            if (!(hi > lo)) {
                val padding = max(0.5, abs(lo) * 0.05)
                lo -= padding
                hi += padding
            }

            val span = hi - lo
            val binWidth = span / bins
            val counts = IntArray(bins)
            var inside = 0
            for (value in sorted) {
                if (value < lo || value > hi) continue
                val index = (((value - lo) / span) * bins).toInt().coerceIn(0, bins - 1)
                counts[index]++
                inside++
            }

            val effectiveCount = max(inside, 1)
            val smoothed = gaussianSmooth(counts, sigma = 1.15)
            density = DoubleArray(bins) { index ->
                smoothed[index] / (effectiveCount * binWidth)
            }
            maxDensity = density.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        }

        val span = hi - lo
        val mean = sorted.sum() / sorted.size
        var squaredDeviations = 0.0
        for (value in sorted) {
            squaredDeviations += (value - mean) * (value - mean)
        }
        val standardDeviation = sqrt(squaredDeviations / sorted.size)
        val sampleXs = DoubleArray(sorted.size) { index ->
            ((sorted[index] - lo) / span).coerceIn(0.0, 1.0)
        }

        return PreparedSequence(
            lo = lo,
            hi = hi,
            density = density,
            maxDensity = maxDensity,
            mean = mean,
            standardDeviation = standardDeviation,
            sampleXs = sampleXs,
            sampleCount = sorted.size,
            isDiscrete = isDiscrete,
            discreteValues = discreteValues
        )
    }

    private fun detectDiscreteRange(sorted: DoubleArray): Pair<Int, Int>? {
        if (sorted.isEmpty()) return null
        val minimum = sorted.first()
        val maximum = sorted.last()
        if (abs(minimum) > 1e12 || abs(maximum) > 1e12) return null
        if (maximum - minimum > MAX_DISCRETE_SUPPORT) return null

        for (value in sorted) {
            if (abs(value - value.roundToLong()) > 1e-9) return null
        }

        return minimum.roundToInt() to maximum.roundToInt()
    }

    private fun quantile(sorted: DoubleArray, probability: Double): Double {
        if (sorted.size == 1) return sorted[0]
        val position = probability.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lower = position.toInt()
        val upper = min(lower + 1, sorted.lastIndex)
        val fraction = position - lower
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction
    }

    private fun gaussianSmooth(values: IntArray, sigma: Double): DoubleArray {
        if (sigma <= 0.0 || values.isEmpty()) {
            return DoubleArray(values.size) { values[it].toDouble() }
        }
        val radius = max(1, ceil(3.0 * sigma).toInt())
        val kernel = DoubleArray(radius * 2 + 1) { offset ->
            exp(-0.5 * ((offset - radius) / sigma) * ((offset - radius) / sigma))
        }
        val kernelSum = kernel.sum()

        return DoubleArray(values.size) { index ->
            var total = 0.0
            for (offset in -radius..radius) {
                val source = index + offset
                if (source in values.indices) {
                    total += values[source] * kernel[offset + radius]
                }
            }
            total / kernelSum
        }
    }

    private data class PreparedSequence(
        val lo: Double,
        val hi: Double,
        val density: DoubleArray,
        val maxDensity: Double,
        val mean: Double,
        val standardDeviation: Double,
        val sampleXs: DoubleArray,
        val sampleCount: Int,
        val isDiscrete: Boolean = false,
        val discreteValues: IntArray? = null
    )

    private class Scene(
        private val graphics: Graphics2D,
        private val width: Int,
        private val height: Int,
        private val prepared: PreparedSequence,
        private val info: SequencePlotInfo
    ) {
        private val depth = 0.32
        private val zWorld = 0.92
        private val cosAngle = cos(PI / 6.0)
        private val sinAngle = sin(PI / 6.0)

        private val plotLeft = width * 0.10
        private val plotTop = height * 0.17
        private val plotRight = width * 0.82
        private val plotBottom = height * 0.89

        private val scale: Double
        private val originX: Double
        private val originY: Double

        private val titleFont = Font("SansSerif", Font.BOLD, (height * 0.036).toInt().coerceAtLeast(18))
        private val subtitleFont = Font("SansSerif", Font.PLAIN, (height * 0.0175).toInt().coerceAtLeast(11))
        private val axisFont = Font("SansSerif", Font.BOLD, (height * 0.019).toInt().coerceAtLeast(11))
        private val tickFont = Font("SansSerif", Font.PLAIN, (height * 0.0155).toInt().coerceAtLeast(9))
        private val footerFont = Font("SansSerif", Font.PLAIN, (height * 0.0135).toInt().coerceAtLeast(9))

        private val textColor = Color(226, 232, 240)
        private val mutedColor = Color(148, 163, 184)
        private val gridColor = Color(71, 85, 105, 64)
        private val axisColor = Color(100, 116, 139, 210)

        init {
            val projected = ArrayList<Point2D.Double>(8)
            for (x in doubleArrayOf(0.0, 1.0)) {
                for (y in doubleArrayOf(0.0, depth)) {
                    for (z in doubleArrayOf(0.0, zWorld)) {
                        projected.add(rawProject(x, y, z))
                    }
                }
            }
            val minX = projected.minOf { it.x }
            val maxX = projected.maxOf { it.x }
            val minY = projected.minOf { it.y }
            val maxY = projected.maxOf { it.y }

            val availableWidth = plotRight - plotLeft
            val availableHeight = plotBottom - plotTop
            scale = min(availableWidth / (maxX - minX), availableHeight / (maxY - minY))
            originX = plotLeft + (availableWidth - (maxX - minX) * scale) / 2.0 - minX * scale
            originY = plotTop + (availableHeight - (maxY - minY) * scale) / 2.0 - minY * scale
        }

        fun draw() {
            drawFloor()
            drawFootprints()
            drawSampleCloud()
            drawBars()
            drawBoxEdges()
            drawXAxis()
            drawZAxis()
            drawColorBar()
            drawText()
        }

        private fun rawProject(x: Double, y: Double, z: Double): Point2D.Double {
            val screenX = (x - y) * cosAngle
            val screenY = (x + y) * sinAngle - z
            return Point2D.Double(screenX, screenY)
        }

        private fun project(x: Double, y: Double, z: Double): Point2D.Double {
            val raw = rawProject(x, y, z)
            return Point2D.Double(originX + raw.x * scale, originY + raw.y * scale)
        }

        private fun drawFloor() {
            fillPolygon(
                listOf(
                    project(0.0, 0.0, 0.0),
                    project(1.0, 0.0, 0.0),
                    project(1.0, depth, 0.0),
                    project(0.0, depth, 0.0)
                ),
                Color(9, 16, 30, 235)
            )

            graphics.stroke = BasicStroke(1f)
            graphics.color = gridColor
            val xDivisions = 8
            for (index in 0..xDivisions) {
                val x = index.toDouble() / xDivisions
                drawLine(project(x, 0.0, 0.0), project(x, depth, 0.0))
            }
            for (index in 0..2) {
                val y = depth * index / 2.0
                drawLine(project(0.0, y, 0.0), project(1.0, y, 0.0))
            }
        }

        private fun drawFootprints() {
            val y0 = depth * 0.12
            val y1 = depth * 0.88
            for (index in prepared.density.indices) {
                val heightRatio = (prepared.density[index] / prepared.maxDensity).coerceIn(0.0, 1.0)
                if (heightRatio <= 0.0) continue
                val color = viridis(heightRatio)
                val x0 = index.toDouble() / prepared.density.size
                val x1 = (index + 1).toDouble() / prepared.density.size
                fillPolygon(
                    listOf(
                        project(x0, y0, 0.0),
                        project(x1, y0, 0.0),
                        project(x1, y1, 0.0),
                        project(x0, y1, 0.0)
                    ),
                    Color(color.red, color.green, color.blue, 44)
                )
            }
        }

        private fun drawSampleCloud() {
            if (prepared.sampleXs.isEmpty()) return

            val maxPoints = 5000
            val step = max(1, prepared.sampleXs.size / maxPoints)
            val random = Random((info.seed ?: 0L) xor 0x5DEECE66DL)
            graphics.color = Color(248, 250, 252, 34)

            var index = 0
            while (index < prepared.sampleXs.size) {
                val x = prepared.sampleXs[index]
                val y = random.nextDouble() * depth
                val point = project(x, y, 0.0)
                graphics.fill(
                    Rectangle2D.Double(point.x - 0.9, point.y - 0.9, 1.8, 1.8)
                )
                index += step
            }
        }

        private fun drawBars() {
            val y0 = depth * 0.12
            val y1 = depth * 0.88
            val gap = if (prepared.isDiscrete) 0.16 else 0.025
            val faces = ArrayList<Face>(prepared.density.size * 3)

            for (index in prepared.density.indices) {
                val heightRatio = (prepared.density[index] / prepared.maxDensity).coerceIn(0.0, 1.0)
                if (heightRatio <= 0.0001) continue

                val x0 = (index + gap) / prepared.density.size
                val x1 = (index + 1 - gap) / prepared.density.size
                val top = heightRatio * zWorld
                val baseColor = viridis(heightRatio)

                faces += newFace(
                    listOf(
                        Triple(x0, y0, top),
                        Triple(x1, y0, top),
                        Triple(x1, y1, top),
                        Triple(x0, y1, top)
                    ),
                    baseColor
                )
                faces += newFace(
                    listOf(
                        Triple(x1, y0, 0.0),
                        Triple(x1, y0, top),
                        Triple(x1, y1, top),
                        Triple(x1, y1, 0.0)
                    ),
                    shade(baseColor, 0.68)
                )
                faces += newFace(
                    listOf(
                        Triple(x0, y1, 0.0),
                        Triple(x1, y1, 0.0),
                        Triple(x1, y1, top),
                        Triple(x0, y1, top)
                    ),
                    shade(baseColor, 0.50)
                )
            }

            // Isometric projection looks along (1, 1, 1): smaller depth is farther away.
            faces.sortBy { it.depth }
            for (face in faces) {
                fillPolygon(face.points, face.color)
            }
        }

        private fun drawBoxEdges() {
            graphics.color = axisColor
            graphics.stroke = BasicStroke(1.4f)
            drawLine(project(0.0, 0.0, 0.0), project(1.0, 0.0, 0.0))
            drawLine(project(1.0, 0.0, 0.0), project(1.0, depth, 0.0))
            drawLine(project(1.0, depth, 0.0), project(0.0, depth, 0.0))
            drawLine(project(0.0, depth, 0.0), project(0.0, 0.0, 0.0))

            graphics.color = Color(71, 85, 105, 115)
            drawLine(project(0.0, 0.0, zWorld), project(1.0, 0.0, zWorld))
            drawLine(project(1.0, 0.0, zWorld), project(1.0, depth, zWorld))
            drawLine(project(1.0, depth, zWorld), project(0.0, depth, zWorld))
            drawLine(project(0.0, depth, zWorld), project(0.0, 0.0, zWorld))

            for (x in doubleArrayOf(0.0, 1.0)) {
                for (y in doubleArrayOf(0.0, depth)) {
                    drawLine(project(x, y, 0.0), project(x, y, zWorld))
                }
            }
        }

        private fun drawXAxis() {
            graphics.font = tickFont
            graphics.color = mutedColor

            val discreteValues = prepared.discreteValues
            if (prepared.isDiscrete && discreteValues != null) {
                val step = max(1, (discreteValues.size + 6) / 7)
                for (index in discreteValues.indices step step) {
                    val fraction = (index + 0.5) / discreteValues.size
                    val point = project(fraction, depth, 0.0)
                    graphics.stroke = BasicStroke(1.2f)
                    drawLine(point, Point2D.Double(point.x, point.y + height * 0.012))

                    val label = discreteValues[index].toString()
                    val labelWidth = graphics.fontMetrics.stringWidth(label)
                    graphics.drawString(
                        label,
                        (point.x - labelWidth / 2.0).toFloat(),
                        (point.y + height * 0.036).toFloat()
                    )
                }
            } else {
                val tickCount = 6
                for (index in 0..tickCount) {
                    val fraction = index.toDouble() / tickCount
                    val point = project(fraction, depth, 0.0)
                    graphics.stroke = BasicStroke(1.2f)
                    drawLine(point, Point2D.Double(point.x, point.y + height * 0.012))

                    val value = prepared.lo + (prepared.hi - prepared.lo) * fraction
                    val label = formatValue(value)
                    val labelWidth = graphics.fontMetrics.stringWidth(label)
                    graphics.drawString(
                        label,
                        (point.x - labelWidth / 2.0).toFloat(),
                        (point.y + height * 0.036).toFloat()
                    )
                }
            }

            graphics.font = axisFont
            graphics.color = textColor
            val center = project(0.5, depth, 0.0)
            drawCentered("Value", center.x, center.y + height * 0.086)
        }

        private fun drawZAxis() {
            graphics.font = tickFont
            graphics.color = mutedColor
            val tickCount = 4

            for (index in 0..tickCount) {
                val fraction = index.toDouble() / tickCount
                val point = project(0.0, depth, fraction * zWorld)
                graphics.stroke = BasicStroke(1.2f)
                drawLine(point, Point2D.Double(point.x - width * 0.009, point.y))

                val value = prepared.maxDensity * fraction
                val label = formatDensity(value)
                val labelWidth = graphics.fontMetrics.stringWidth(label)
                graphics.drawString(
                    label,
                    (point.x - width * 0.016 - labelWidth).toFloat(),
                    (point.y + graphics.fontMetrics.ascent / 3.0).toFloat()
                )
            }
        }

        private fun drawColorBar() {
            val barX = width * 0.885
            val barY = height * 0.28
            val barWidth = width * 0.020
            val barHeight = height * 0.40
            val steps = 160

            for (index in 0 until steps) {
                val fraction = index.toDouble() / (steps - 1)
                graphics.color = viridis(fraction)
                val y = barY + barHeight * (1.0 - fraction)
                graphics.fill(
                    Rectangle2D.Double(barX, y, barWidth, barHeight / steps + 1.2)
                )
            }

            graphics.color = Color(71, 85, 105, 220)
            graphics.stroke = BasicStroke(1.2f)
            graphics.draw(Rectangle2D.Double(barX, barY, barWidth, barHeight))

            graphics.font = axisFont
            graphics.color = textColor
            val valueLabel = if (prepared.isDiscrete) "Probability" else "Density"
            graphics.drawString(valueLabel, barX.toFloat(), (barY - height * 0.022).toFloat())

            graphics.font = tickFont
            graphics.color = mutedColor
            val tickCount = 4
            for (index in 0..tickCount) {
                val fraction = index.toDouble() / tickCount
                val y = barY + barHeight * (1.0 - fraction)
                graphics.stroke = BasicStroke(1.2f)
                drawLine(
                    Point2D.Double(barX + barWidth, y),
                    Point2D.Double(barX + barWidth + width * 0.008, y)
                )
                val label = formatDensity(prepared.maxDensity * fraction)
                graphics.drawString(
                    label,
                    (barX + barWidth + width * 0.013).toFloat(),
                    (y + graphics.fontMetrics.ascent / 3.0).toFloat()
                )
            }
        }

        private fun drawText() {
            val title = "${prettyDistribution(info.distribution)} Distribution"
            graphics.font = titleFont
            graphics.color = Color(248, 250, 252)
            drawCentered(title, width / 2.0, height * 0.065)

            val params = listOfNotNull(
                info.param1?.let { "param1 = ${formatValue(it)}" },
                info.param2?.let { "param2 = ${formatValue(it)}" },
                info.param3?.let { "param3 = ${formatValue(it)}" }
            )
            val parameterText = if (params.isEmpty()) {
                "${info.distribution}  ·  default parameters"
            } else {
                "${info.distribution}  ·  ${params.joinToString("  ·  ")}"
            }

            graphics.font = subtitleFont
            graphics.color = Color(147, 197, 253)
            drawCentered(parameterText, width / 2.0, height * 0.103)

            val stats = "n = ${String.format(Locale.ROOT, "%,d", prepared.sampleCount)}  ·  " +
                "mean = ${formatValue(prepared.mean)}  ·  " +
                "sd = ${formatValue(prepared.standardDeviation)}  ·  " +
                "seed ${info.seed ?: "n/a"}"
            graphics.color = mutedColor
            drawCentered(stats, width / 2.0, height * 0.132)

            graphics.font = footerFont
            graphics.color = Color(100, 116, 139)
            drawCentered(
                "Generated with PseudorandomometricSequencesApi  ·  GET /api/v1/random/sequence/plot",
                width / 2.0,
                height * 0.972
            )
        }

        private fun drawCentered(text: String, centerX: Double, baselineY: Double) {
            val textWidth = graphics.fontMetrics.stringWidth(text)
            graphics.drawString(text, (centerX - textWidth / 2.0).toFloat(), baselineY.toFloat())
        }

        private fun drawLine(from: Point2D.Double, to: Point2D.Double) {
            graphics.draw(Line2D.Double(from, to))
        }

        private fun fillPolygon(points: List<Point2D.Double>, color: Color) {
            if (points.size < 3) return
            val path = Path2D.Double()
            path.moveTo(points[0].x, points[0].y)
            for (index in 1 until points.size) {
                path.lineTo(points[index].x, points[index].y)
            }
            path.closePath()
            graphics.color = color
            graphics.fill(path)
        }

        private fun newFace(
            points: List<Triple<Double, Double, Double>>,
            color: Color
        ): Face {
            val projected = points.map { project(it.first, it.second, it.third) }
            val depth = points.sumOf { it.first + it.second + it.third } / points.size
            return Face(projected, depth, color)
        }

        private data class Face(
            val points: List<Point2D.Double>,
            val depth: Double,
            val color: Color
        )
    }
}

private fun shade(color: Color, factor: Double): Color = Color(
    (color.red * factor).roundToInt().coerceIn(0, 255),
    (color.green * factor).roundToInt().coerceIn(0, 255),
    (color.blue * factor).roundToInt().coerceIn(0, 255)
)

private fun prettyDistribution(name: String): String =
    name.split('-', '_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            when (part.lowercase()) {
                "gev" -> "GEV"
                "f" -> "F"
                "t" -> "T"
                else -> part.replaceFirstChar { it.uppercase() }
            }
        }
        .ifBlank { "Sequence" }

private fun formatValue(value: Double): String {
    if (!value.isFinite()) return "-"
    val magnitude = abs(value)
    return when {
        magnitude == 0.0 -> "0"
        magnitude >= 10_000 || magnitude < 0.001 -> String.format(Locale.ROOT, "%.2e", value)
        magnitude >= 100 -> String.format(Locale.ROOT, "%.0f", value)
        magnitude >= 1 -> String.format(Locale.ROOT, "%.2f", value)
        else -> String.format(Locale.ROOT, "%.3f", value)
    }
}

private fun formatDensity(value: Double): String {
    if (!value.isFinite()) return "-"
    return when {
        value == 0.0 -> "0"
        abs(value) >= 100 -> String.format(Locale.ROOT, "%.0f", value)
        abs(value) >= 1 -> String.format(Locale.ROOT, "%.2f", value)
        else -> String.format(Locale.ROOT, "%.3f", value)
    }
}

private val VIRIDIS_STOPS = arrayOf(
    intArrayOf(68, 1, 84),
    intArrayOf(72, 40, 120),
    intArrayOf(62, 74, 137),
    intArrayOf(49, 104, 142),
    intArrayOf(38, 130, 142),
    intArrayOf(31, 158, 137),
    intArrayOf(53, 183, 121),
    intArrayOf(109, 205, 89),
    intArrayOf(180, 222, 44),
    intArrayOf(253, 231, 37)
)

private fun viridis(fraction: Double): Color {
    val position = fraction.coerceIn(0.0, 1.0) * (VIRIDIS_STOPS.size - 1)
    val lower = position.toInt().coerceIn(0, VIRIDIS_STOPS.lastIndex)
    val upper = min(lower + 1, VIRIDIS_STOPS.lastIndex)
    val mix = position - lower
    val from = VIRIDIS_STOPS[lower]
    val to = VIRIDIS_STOPS[upper]
    return Color(
        (from[0] + (to[0] - from[0]) * mix).roundToInt(),
        (from[1] + (to[1] - from[1]) * mix).roundToInt(),
        (from[2] + (to[2] - from[2]) * mix).roundToInt()
    )
}
