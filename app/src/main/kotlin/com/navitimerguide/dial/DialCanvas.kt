package com.navitimerguide.dial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.navitimerguide.viewmodel.ChronoState
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * Three stacked layers:
 *  1. [StaticDial]      — dial face (no rotation, no live time).
 *  2. [RotatingBezel]   — outer slide-rule scale; whole layer rotated via
 *                         graphicsLayer rotationZ.
 *  3. [LiveHandsLayer]  — hour, minute, central red chronograph hand,
 *                         small running seconds, 30-min and 12-hr chrono
 *                         counters. Driven by the system clock and the
 *                         chronograph state.
 */
@Composable
fun WatchDial(
    bezelRotationDegrees: Double,
    chronoState: ChronoState,
    chronoMillisProvider: () -> Long,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
    ) {
        StaticDial(measurer = measurer, modifier = Modifier.fillMaxSize())
        RotatingBezel(
            measurer = measurer,
            rotationDegrees = bezelRotationDegrees,
            modifier = Modifier.fillMaxSize()
        )
        LiveHandsLayer(
            chronoState = chronoState,
            chronoMillisProvider = chronoMillisProvider,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// =============================================================== layers

@Composable
private fun StaticDial(measurer: TextMeasurer, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val g = geom()
        drawCoinEdgeBaseplate(g)
        drawBezelInsertRecess(g)
        drawFixedChapterRing(g, measurer)
        drawDialBackground(g)
        drawSunburstOverlay(g)
        drawDialHighlight(g)
        drawBrandMarks(g, measurer)
        drawSubDialFaces(g, measurer)
        drawDialHourIndices(g)
        drawCrownAndPushers(g)
    }
}

@Composable
private fun RotatingBezel(
    measurer: TextMeasurer,
    rotationDegrees: Double,
    modifier: Modifier
) {
    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotationDegrees.toFloat() }) {
        val g = geom()
        drawRotatingBezelScale(g, measurer)
    }
}

@Composable
private fun LiveHandsLayer(
    chronoState: ChronoState,
    chronoMillisProvider: () -> Long,
    modifier: Modifier
) {
    val nowState: State<LocalDateTime> = produceState(initialValue = currentLocalDateTime()) {
        while (true) {
            value = currentLocalDateTime()
            delay(if (chronoState == ChronoState.RUNNING) 50L else 250L)
        }
    }
    val now = nowState.value
    Canvas(modifier = modifier) {
        val g = geom()
        val chronoMs = chronoMillisProvider()
        drawSubDialSecondsHand(g, now)
        drawChronoMinAndHourHands(g, chronoMs)
        drawTimeHands(g, now)
        drawChronoSecondsHand(g, chronoMs)
        drawHandHub(g)
    }
}

private fun currentLocalDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

// =============================================================== geometry

private data class DialGeom(
    val w: Float, val h: Float,
    val cx: Float, val cy: Float,
    val center: Offset,
    val rOuter: Float,                 // outer edge of chrome bezel (the case rim)
    val rBezelOuter: Float,            // outer edge of black slide-rule insert
    val rBezelInner: Float,            // inner edge of slide-rule insert
    val rChapterOuter: Float,
    val rChapterInner: Float,
    val rDial: Float
)

private fun DrawScope.geom(): DialGeom {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    // Shrink the watch a touch so the crown + pushers fit inside the canvas.
    val rOuter = (minOf(w, h) / 2f) * 0.92f
    val rBezelOuter = rOuter * 0.99f
    val rBezelInner = rOuter * 0.86f
    val rChapterOuter = rOuter * 0.84f
    val rChapterInner = rOuter * 0.71f
    val rDial = rChapterInner
    return DialGeom(w, h, cx, cy, Offset(cx, cy), rOuter, rBezelOuter, rBezelInner, rChapterOuter, rChapterInner, rDial)
}

// =============================================================== labels

/** Outer rotating bezel: every 5 in the upper half, every 1 in 10..25, every 5 in 30..55. */
private val OUTER_LABEL_SET: Set<Int> =
    (10..25).toSet() +
    setOf(30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95)

/** Inner fixed scale: like the outer, but in the upper half only every 10 (with 70/80/90 abbreviated as 7/8/9). */
private val INNER_LABEL_MAP: Map<Int, String> =
    (10..25).associateWith { it.toString() } +
    listOf(30, 35, 40, 45, 50, 55).associateWith { it.toString() } +
    mapOf(60 to "60", 70 to "7", 80 to "8", 90 to "9")

// =============================================================== ticks

private enum class TickRank { TALL, MEDIUM, SHORT }

/** Step between ticks at scale-value [v], following the slide-rule's progressive subdivision. */
private fun stepAt(v: Double): Double = when {
    v < 20.0 -> 0.1
    v < 25.0 -> 0.2
    v < 50.0 -> 0.5
    else -> 1.0
}

private fun isInteger(v: Double): Boolean = kotlin.math.abs(v - round(v)) < 1e-6
private fun isHalfStep(v: Double): Boolean = kotlin.math.abs((v * 2.0) - round(v * 2.0)) < 1e-6 && !isInteger(v)

private fun tickRank(v: Double, isLabelled: Boolean): TickRank {
    if (isLabelled) return TickRank.TALL
    return when {
        v < 20.0 -> if (isHalfStep(v)) TickRank.MEDIUM else TickRank.SHORT  // x.5 stands out
        v < 25.0 -> TickRank.SHORT
        v < 50.0 -> if (isInteger(v)) TickRank.MEDIUM else TickRank.SHORT   // integers stand out
        else -> TickRank.SHORT
    }
}

/** Ordered list of all tick values across one decade [10, 100). */
private fun allTickValues(): List<Double> {
    val out = mutableListOf<Double>()
    var v = 10.0
    while (v < 100.0 - 1e-9) {
        out += v
        v = round((v + stepAt(v)) * 1000.0) / 1000.0
    }
    return out
}

// =============================================================== chrome teeth

private fun DrawScope.drawCoinEdgeBaseplate(g: DialGeom) {
    val teeth = 90
    val rTip = g.rOuter
    val rBase = g.rOuter * 0.94f
    drawCircle(color = Color(0xFF0C0C0E), radius = g.rOuter, center = g.center)
    for (i in 0 until teeth) {
        val angle = i * (360.0 / teeth)
        val rad = angle * PI / 180.0
        val perpRad = rad + PI / 2
        val toothHalf = g.rOuter * 0.014f
        val tipX = g.center.x + (rTip * cos(rad)).toFloat()
        val tipY = g.center.y + (rTip * sin(rad)).toFloat()
        val baseX = g.center.x + (rBase * cos(rad)).toFloat()
        val baseY = g.center.y + (rBase * sin(rad)).toFloat()
        val px = (toothHalf * cos(perpRad)).toFloat()
        val py = (toothHalf * sin(perpRad)).toFloat()

        drawLine(color = DialPalette.SteelGroove,
            start = Offset(baseX + px * 1.3f, baseY + py * 1.3f),
            end = Offset(tipX + px * 1.3f, tipY + py * 1.3f),
            strokeWidth = toothHalf * 0.65f)
        drawLine(color = DialPalette.SteelLight,
            start = Offset(baseX, baseY),
            end = Offset(tipX, tipY),
            strokeWidth = toothHalf * 0.75f)
        drawLine(color = DialPalette.SteelMid,
            start = Offset(baseX - px * 0.6f, baseY - py * 0.6f),
            end = Offset(tipX - px * 0.6f, tipY - py * 0.6f),
            strokeWidth = toothHalf * 0.55f)
    }
    drawCircle(color = DialPalette.SteelLight, radius = g.rOuter, center = g.center, style = Stroke(width = 1.2f))
    drawCircle(color = DialPalette.BezelEdgeShadow, radius = rBase, center = g.center,
        style = Stroke(width = g.rOuter * 0.008f))
}

private fun DrawScope.drawBezelInsertRecess(g: DialGeom) {
    drawCircle(color = DialPalette.BezelInsertBlack, radius = g.rBezelOuter, center = g.center)
    drawCircle(color = Color(0xFF1B1B1B), radius = g.rBezelOuter, center = g.center,
        style = Stroke(width = g.rOuter * 0.006f))
    drawCircle(color = DialPalette.BezelEdgeShadow, radius = g.rBezelInner, center = g.center,
        style = Stroke(width = g.rOuter * 0.012f))
}

// =============================================================== rotating bezel scale (outer)

private fun DrawScope.drawRotatingBezelScale(g: DialGeom, measurer: TextMeasurer) {
    val ringMid = (g.rBezelOuter + g.rBezelInner) / 2f
    val ringWidth = g.rBezelOuter - g.rBezelInner

    val tallLen = ringWidth * 0.42f
    val medLen = ringWidth * 0.28f
    val shortLen = ringWidth * 0.18f
    val tickOuterR = ringMid + ringWidth * 0.42f

    for (v in allTickValues()) {
        val intV = round(v).toInt()
        val isLabelled = isInteger(v) && intV in OUTER_LABEL_SET
        val rank = tickRank(v, isLabelled)
        val angle = DialMath.drawAngleDeg(v)
        val rad = angle * PI / 180.0

        val len = when (rank) {
            TickRank.TALL -> tallLen
            TickRank.MEDIUM -> medLen
            TickRank.SHORT -> shortLen
        }
        val sw = when (rank) {
            TickRank.TALL -> 1.6f
            TickRank.MEDIUM -> 1.0f
            TickRank.SHORT -> 0.6f
        }
        val tickInnerR = tickOuterR - len
        val sx = g.center.x + (tickInnerR * cos(rad)).toFloat()
        val sy = g.center.y + (tickInnerR * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuterR * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuterR * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (rank == TickRank.SHORT) 0.55f else 0.95f),
            start = Offset(sx, sy), end = Offset(ex, ey),
            strokeWidth = sw
        )
        if (isLabelled) {
            val labelR = ringMid + ringWidth * 0.05f
            val isRed = (intV == 60 || intV == 10 || intV == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = intV.toString(),
                angleDegFromTop = angle.toFloat(),
                radius = labelR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * (if (intV % 5 == 0) 0.058f else 0.040f) / density).sp
            )
        }
    }
    // Red triangles on outer at 10 / 36 / 60
    listOf(10.0, 36.0, 60.0).forEach { v ->
        val angle = DialMath.drawAngleDeg(v)
        drawTriangleAtAngle(
            center = g.center, angleDeg = angle.toFloat(),
            radius = ringMid + ringWidth * 0.40f, size = ringWidth * 0.18f,
            color = DialPalette.Red, inward = true
        )
    }
}

// =============================================================== fixed chapter ring (inner)

private fun DrawScope.drawFixedChapterRing(g: DialGeom, measurer: TextMeasurer) {
    val midR = (g.rChapterOuter + g.rChapterInner) / 2f
    val width = g.rChapterOuter - g.rChapterInner

    drawCircle(color = Color(0xFF050505), radius = g.rChapterOuter, center = g.center)
    drawCircle(color = Color(0xFF1F1F1F), radius = g.rChapterOuter, center = g.center,
        style = Stroke(width = width * 0.05f))

    val tallLen = width * 0.42f
    val medLen = width * 0.28f
    val shortLen = width * 0.18f
    val tickOuterR = midR + width * 0.42f

    for (v in allTickValues()) {
        val intV = round(v).toInt()
        val isLabelled = isInteger(v) && intV in INNER_LABEL_MAP
        val rank = tickRank(v, isLabelled)
        val angle = DialMath.drawAngleDeg(v)
        val rad = angle * PI / 180.0

        val len = when (rank) {
            TickRank.TALL -> tallLen
            TickRank.MEDIUM -> medLen
            TickRank.SHORT -> shortLen
        }
        val sw = when (rank) {
            TickRank.TALL -> 1.4f
            TickRank.MEDIUM -> 0.9f
            TickRank.SHORT -> 0.55f
        }
        val tickInnerR = tickOuterR - len
        val sx = g.center.x + (tickInnerR * cos(rad)).toFloat()
        val sy = g.center.y + (tickInnerR * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuterR * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuterR * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (rank == TickRank.SHORT) 0.4f else 0.9f),
            start = Offset(sx, sy), end = Offset(ex, ey),
            strokeWidth = sw
        )
        if (isLabelled) {
            val text = INNER_LABEL_MAP.getValue(intV)
            val isRed = (intV == 60 || intV == 10 || intV == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = text,
                angleDegFromTop = angle.toFloat(),
                radius = midR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * (if (intV % 5 == 0) 0.048f else 0.034f) / density).sp
            )
        }
    }

    // KM (text only, no triangle), STAT/NAUT (triangle + text)
    val markerLabelR = midR - width * 0.32f
    Markers.all.filter { it.side == ScaleSide.INNER && it.style != MarkerStyle.RED_NUMERAL }
        .forEach { m ->
            val angle = DialMath.drawAngleDeg(m.scaleValue)
            if (m.style == MarkerStyle.TRIANGLE_OUTWARD) {
                drawTriangleAtAngle(
                    center = g.center, angleDeg = angle.toFloat(),
                    radius = midR + width * 0.40f, size = width * 0.18f,
                    color = DialPalette.Red, inward = false
                )
            }
            m.text?.let { txt ->
                drawScaleNumeralUpright(
                    measurer = measurer,
                    text = txt,
                    angleDegFromTop = angle.toFloat(),
                    radius = markerLabelR,
                    center = g.center,
                    color = DialPalette.Red,
                    sizeSp = (g.rOuter * 0.038f / density).sp,
                    bold = true
                )
            }
        }
}

// =============================================================== dial background

private fun DrawScope.drawDialBackground(g: DialGeom) {
    val brush = Brush.radialGradient(
        colors = listOf(DialPalette.DialGreenInner, DialPalette.DialGreenSpokeDark, DialPalette.DialGreenOuter),
        center = g.center, radius = g.rDial
    )
    drawCircle(brush = brush, radius = g.rDial, center = g.center)
}

private fun DrawScope.drawSunburstOverlay(g: DialGeom) {
    val spokes = 240
    val rIn = g.rDial * 0.04f
    val rOut = g.rDial * 0.98f
    for (i in 0 until spokes) {
        val angle = i * (360.0 / spokes)
        val rad = angle * PI / 180.0
        val sx = g.center.x + (rIn * cos(rad)).toFloat()
        val sy = g.center.y + (rIn * sin(rad)).toFloat()
        val ex = g.center.x + (rOut * cos(rad)).toFloat()
        val ey = g.center.y + (rOut * sin(rad)).toFloat()
        val light = i % 2 == 0
        drawLine(
            color = if (light) DialPalette.DialGreenSpokeLight.copy(alpha = 0.18f)
                    else DialPalette.DialGreenSpokeDark.copy(alpha = 0.20f),
            start = Offset(sx, sy), end = Offset(ex, ey),
            strokeWidth = 1.0f
        )
    }
}

private fun DrawScope.drawDialHighlight(g: DialGeom) {
    val glow = Brush.radialGradient(
        colors = listOf(Color(0x33FFFFFF), Color(0x11FFFFFF), Color(0x00FFFFFF)),
        center = Offset(g.center.x - g.rDial * 0.45f, g.center.y - g.rDial * 0.55f),
        radius = g.rDial * 0.85f
    )
    drawCircle(brush = glow, radius = g.rDial, center = g.center)
}

// =============================================================== brand marks + winged anchor

private fun DrawScope.drawBrandMarks(g: DialGeom, measurer: TextMeasurer) {
    val anchorY = g.center.y - g.rDial * 0.34f
    val anchorScale = g.rDial * 0.13f
    drawWingedAnchor(Offset(g.center.x, anchorY), anchorScale)

    drawCenteredText(measurer, "BREITLING",
        TextStyle(color = DialPalette.Numeral, fontSize = (g.rDial * 0.105f / density).sp,
            fontWeight = FontWeight.Bold, letterSpacing = (g.rDial * 0.006f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.17f))

    drawCenteredText(measurer, "1884",
        TextStyle(color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (g.rDial * 0.05f / density).sp, fontWeight = FontWeight.Medium,
            letterSpacing = (g.rDial * 0.012f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.075f))

    drawCenteredText(measurer, "NAVITIMER",
        TextStyle(color = DialPalette.Numeral, fontSize = (g.rDial * 0.062f / density).sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (g.rDial * 0.006f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.012f))

    drawCenteredText(measurer, "SWISS  MADE",
        TextStyle(color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (g.rDial * 0.034f / density).sp, fontWeight = FontWeight.Medium,
            letterSpacing = (g.rDial * 0.006f / density).sp),
        Offset(g.center.x, g.center.y + g.rDial * 0.83f))
}

private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer, text: String, style: TextStyle, centerTopLeft: Offset
) {
    val l = measurer.measure(androidx.compose.ui.text.AnnotatedString(text), style)
    drawText(textLayoutResult = l,
        topLeft = Offset(centerTopLeft.x - l.size.width / 2f, centerTopLeft.y))
}

private fun DrawScope.drawWingedAnchor(c: Offset, scale: Float) {
    val color = DialPalette.Numeral.copy(alpha = 0.95f)
    // Three stacked feathered wing arcs, each side
    for (k in 0..2) {
        val r = scale * (1.6f - k * 0.18f)
        val sweep = 60f - k * 8f
        val startLeft = 200f + k * 4f
        val startRight = 360f - startLeft - sweep
        drawArc(
            color = color, startAngle = startLeft, sweepAngle = sweep, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r * 0.55f),
            size = Size(r * 2, r * 1.1f),
            style = Stroke(width = scale * (0.085f - k * 0.012f))
        )
        drawArc(
            color = color, startAngle = startRight, sweepAngle = sweep, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r * 0.55f),
            size = Size(r * 2, r * 1.1f),
            style = Stroke(width = scale * (0.085f - k * 0.012f))
        )
    }
    // Anchor body
    drawCircle(color = color, center = Offset(c.x, c.y - scale * 0.38f),
        radius = scale * 0.13f, style = Stroke(width = scale * 0.07f))
    drawLine(color = color,
        start = Offset(c.x, c.y - scale * 0.25f),
        end = Offset(c.x, c.y + scale * 0.95f),
        strokeWidth = scale * 0.08f)
    drawLine(color = color,
        start = Offset(c.x - scale * 0.62f, c.y + scale * 0.05f),
        end = Offset(c.x + scale * 0.62f, c.y + scale * 0.05f),
        strokeWidth = scale * 0.07f)
    // Curved flukes
    val flukePath = Path().apply {
        moveTo(c.x - scale * 0.62f, c.y + scale * 0.45f)
        cubicTo(
            c.x - scale * 0.62f, c.y + scale * 0.95f,
            c.x - scale * 0.20f, c.y + scale * 1.10f,
            c.x, c.y + scale * 1.05f
        )
        cubicTo(
            c.x + scale * 0.20f, c.y + scale * 1.10f,
            c.x + scale * 0.62f, c.y + scale * 0.95f,
            c.x + scale * 0.62f, c.y + scale * 0.45f
        )
    }
    drawPath(flukePath, color = color, style = Stroke(width = scale * 0.07f))
    // Centre rivet
    drawCircle(color = color, center = Offset(c.x, c.y + scale * 0.05f), radius = scale * 0.07f)
}

// =============================================================== sub-dials

private fun DrawScope.drawSubDialFaces(g: DialGeom, measurer: TextMeasurer) {
    val subR = g.rDial * 0.22f
    val offset = g.rDial * 0.42f
    // 9 o'clock — small running seconds (60-second face)
    drawSubDialFace(
        center = Offset(g.center.x - offset, g.center.y), radius = subR,
        ticks = 60, majorEvery = 5, measurer = measurer,
        ringNumbers = listOf(20 to "20", 40 to "40")
    )
    // 3 o'clock — 30-min chrono counter
    drawSubDialFace(
        center = Offset(g.center.x + offset, g.center.y), radius = subR,
        ticks = 30, majorEvery = 5, measurer = measurer,
        ringNumbers = listOf(10 to "10", 20 to "20", 30 to "30")
    )
    // 6 o'clock — 12-hr chrono counter (no "6" label; that's where the date sits)
    val hrCenter = Offset(g.center.x, g.center.y + offset)
    drawSubDialFace(
        center = hrCenter, radius = subR,
        ticks = 12, majorEvery = 3, measurer = measurer,
        ringNumbers = listOf(3 to "3", 9 to "9", 12 to "12")
    )
    // Date window inside the 12-hr sub-dial, at the 6 o'clock position (BELOW centre)
    val now = currentLocalDateTime()
    val dateBoxW = subR * 0.65f
    val dateBoxH = subR * 0.32f
    val dateTopLeft = Offset(hrCenter.x - dateBoxW / 2f, hrCenter.y + subR * 0.32f)
    drawRect(color = DialPalette.HandFrame,
        topLeft = Offset(dateTopLeft.x - 1.2f, dateTopLeft.y - 1.2f),
        size = Size(dateBoxW + 2.4f, dateBoxH + 2.4f))
    drawRect(color = Color.White, topLeft = dateTopLeft, size = Size(dateBoxW, dateBoxH))
    val l = measurer.measure(
        androidx.compose.ui.text.AnnotatedString(now.dayOfMonth.toString()),
        TextStyle(color = Color.Black, fontSize = (subR * 0.4f / density).sp,
            fontWeight = FontWeight.SemiBold)
    )
    drawText(textLayoutResult = l,
        topLeft = Offset(
            dateTopLeft.x + (dateBoxW - l.size.width) / 2f,
            dateTopLeft.y + (dateBoxH - l.size.height) / 2f
        ))
}

private fun DrawScope.drawSubDialFace(
    center: Offset, radius: Float, ticks: Int, majorEvery: Int,
    ringNumbers: List<Pair<Int, String>>, measurer: TextMeasurer
) {
    drawCircle(color = DialPalette.SubdialBlack, radius = radius, center = center)
    drawCircle(color = Color(0xFF1A1A1A), radius = radius, center = center,
        style = Stroke(width = radius * 0.04f))
    // Concentric guilloché rings
    val rings = 14
    for (k in 1..rings) {
        val r = radius * (0.06f + 0.86f * k / rings)
        drawCircle(color = DialPalette.SubdialAzureTick.copy(alpha = 0.55f),
            radius = r, center = center, style = Stroke(width = 0.6f))
    }
    for (i in 0 until ticks) {
        val angle = i * (360.0 / ticks) - 90.0
        val rad = angle * PI / 180.0
        val isMajor = i % majorEvery == 0
        val rIn = radius * (if (isMajor) 0.78f else 0.86f)
        val rOut = radius * 0.94f
        val sx = center.x + (rIn * cos(rad)).toFloat()
        val sy = center.y + (rIn * sin(rad)).toFloat()
        val ex = center.x + (rOut * cos(rad)).toFloat()
        val ey = center.y + (rOut * sin(rad)).toFloat()
        drawLine(color = DialPalette.SubdialTick.copy(alpha = if (isMajor) 0.95f else 0.5f),
            start = Offset(sx, sy), end = Offset(ex, ey),
            strokeWidth = if (isMajor) radius * 0.025f else radius * 0.012f)
    }
    for ((tick, txt) in ringNumbers) {
        val angle = tick * (360.0 / ticks) - 90.0
        val rad = angle * PI / 180.0
        val rL = radius * 0.62f
        val tx = center.x + (rL * cos(rad)).toFloat()
        val ty = center.y + (rL * sin(rad)).toFloat()
        val l = measurer.measure(
            androidx.compose.ui.text.AnnotatedString(txt),
            TextStyle(color = DialPalette.Numeral, fontSize = (radius * 0.22f / density).sp,
                fontWeight = FontWeight.Medium)
        )
        drawText(textLayoutResult = l,
            topLeft = Offset(tx - l.size.width / 2f, ty - l.size.height / 2f))
    }
    drawCircle(color = DialPalette.Hand, radius = radius * 0.05f, center = center)
}

private fun DrawScope.drawSubDialHand(
    center: Offset, length: Float, angleDeg: Float, color: Color, thickness: Float
) {
    val rad = (angleDeg - 90.0) * PI / 180.0
    val ex = center.x + (length * cos(rad)).toFloat()
    val ey = center.y + (length * sin(rad)).toFloat()
    drawLine(color = color, start = center, end = Offset(ex, ey), strokeWidth = thickness)
}

private fun DrawScope.drawSubDialSecondsHand(g: DialGeom, now: LocalDateTime) {
    val subR = g.rDial * 0.22f
    val offset = g.rDial * 0.42f
    val secondsCenter = Offset(g.center.x - offset, g.center.y)
    val s = now.second + now.nanosecond / 1e9
    val angle = (s * 6.0).toFloat()
    drawSubDialHand(secondsCenter, subR * 0.85f, angle, DialPalette.Hand, subR * 0.04f)
}

private fun DrawScope.drawChronoMinAndHourHands(g: DialGeom, chronoMs: Long) {
    val subR = g.rDial * 0.22f
    val offset = g.rDial * 0.42f
    val totalSec = chronoMs / 1000.0
    val minutes = (totalSec / 60.0) % 30.0           // 30-min counter
    val hours = (totalSec / 3600.0) % 12.0           // 12-hr counter
    val minAngle = (minutes / 30.0 * 360.0).toFloat()
    val hrAngle = (hours / 12.0 * 360.0).toFloat()
    drawSubDialHand(
        center = Offset(g.center.x + offset, g.center.y),
        length = subR * 0.80f, angleDeg = minAngle,
        color = DialPalette.Hand, thickness = subR * 0.04f
    )
    drawSubDialHand(
        center = Offset(g.center.x, g.center.y + offset),
        length = subR * 0.78f, angleDeg = hrAngle,
        color = DialPalette.Hand, thickness = subR * 0.04f
    )
}

// =============================================================== applied hour indices

private fun DrawScope.drawDialHourIndices(g: DialGeom) {
    val skip = setOf(3, 6, 9)
    for (h in 0 until 12) {
        if (h in skip) continue
        val angle = h * 30.0 - 90.0
        val rad = angle * PI / 180.0
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val rIn = g.rDial * 0.62f
        val rOut = g.rDial * 0.74f
        val frameW = g.rDial * 0.030f
        val lumeW = g.rDial * 0.020f
        fun batonPath(width: Float): Path {
            val perpX = (-sinA) * width
            val perpY = cosA * width
            val tipX = g.center.x + (rOut * cos(rad)).toFloat()
            val tipY = g.center.y + (rOut * sin(rad)).toFloat()
            val baseX = g.center.x + (rIn * cos(rad)).toFloat()
            val baseY = g.center.y + (rIn * sin(rad)).toFloat()
            return Path().apply {
                moveTo(tipX + perpX, tipY + perpY)
                lineTo(tipX - perpX, tipY - perpY)
                lineTo(baseX - perpX, baseY - perpY)
                lineTo(baseX + perpX, baseY + perpY)
                close()
            }
        }
        drawPath(path = batonPath(frameW), color = DialPalette.HandFrame)
        drawPath(path = batonPath(lumeW), color = DialPalette.Lume)
        drawPath(path = batonPath(frameW), color = Color(0xFF20242A), style = Stroke(width = 0.8f))
    }
}

// =============================================================== hands (time + chrono)

private fun DrawScope.drawTimeHands(g: DialGeom, now: LocalDateTime) {
    val s = now.second + now.nanosecond / 1e9
    val mFull = now.minute + s / 60.0
    val hFull = (now.hour % 12) + mFull / 60.0
    val hourAngle = (hFull * 30.0).toFloat()
    val minAngle = (mFull * 6.0).toFloat()
    drawBatonHand(g.center, hourAngle, length = g.rDial * 0.42f,
        outerW = g.rDial * 0.045f, innerW = g.rDial * 0.025f)
    drawBatonHand(g.center, minAngle, length = g.rDial * 0.62f,
        outerW = g.rDial * 0.035f, innerW = g.rDial * 0.018f)
}

/**
 * Central red chronograph seconds hand, including a counterweight tail.
 * Frozen at 12 when chrono is idle; sweeps continuously when running.
 */
private fun DrawScope.drawChronoSecondsHand(g: DialGeom, chronoMs: Long) {
    val secs = (chronoMs / 1000.0) % 60.0
    val angleDeg = (secs * 6.0)  // 360° / 60 sec
    val rad = (angleDeg - 90.0) * PI / 180.0
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val tipLen = g.rDial * 0.66f
    val tailLen = g.rDial * 0.18f
    val tipX = g.center.x + tipLen * cosA
    val tipY = g.center.y + tipLen * sinA
    val tailX = g.center.x - tailLen * cosA
    val tailY = g.center.y - tailLen * sinA
    drawLine(color = DialPalette.SecondHand, start = Offset(tailX, tailY), end = Offset(tipX, tipY),
        strokeWidth = g.rDial * 0.013f)
    // Teardrop counterweight
    val cwX = g.center.x - tailLen * 0.7f * cosA
    val cwY = g.center.y - tailLen * 0.7f * sinA
    drawCircle(color = DialPalette.SecondHand, radius = g.rDial * 0.024f, center = Offset(cwX, cwY))
}

private fun DrawScope.drawHandHub(g: DialGeom) {
    drawCircle(color = DialPalette.HandFrame, radius = g.rDial * 0.030f, center = g.center)
    drawCircle(color = DialPalette.Hand, radius = g.rDial * 0.025f, center = g.center)
    drawCircle(color = DialPalette.SecondHand, radius = g.rDial * 0.012f, center = g.center)
}

private fun DrawScope.drawBatonHand(
    center: Offset, angleDeg: Float, length: Float, outerW: Float, innerW: Float
) {
    val rad = (angleDeg - 90.0) * PI / 180.0
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val tipX = center.x + length * cosA
    val tipY = center.y + length * sinA
    val backLen = length * 0.18f
    val backX = center.x - backLen * cosA
    val backY = center.y - backLen * sinA
    fun handPath(w: Float): Path {
        val perpX = -sinA * w
        val perpY = cosA * w
        val tipFlatLen = length * 0.08f
        val tipPreX = center.x + (length - tipFlatLen) * cosA
        val tipPreY = center.y + (length - tipFlatLen) * sinA
        return Path().apply {
            moveTo(backX + perpX, backY + perpY)
            lineTo(backX - perpX, backY - perpY)
            lineTo(tipPreX - perpX, tipPreY - perpY)
            lineTo(tipX, tipY)
            lineTo(tipPreX + perpX, tipPreY + perpY)
            close()
        }
    }
    drawPath(handPath(outerW), color = DialPalette.HandFrame)
    drawPath(handPath(innerW), color = DialPalette.Lume)
    drawPath(handPath(outerW), color = Color(0xFF1A1A1A), style = Stroke(width = 0.8f))
}

// =============================================================== crown + pushers (decorative)

private fun DrawScope.drawCrownAndPushers(g: DialGeom) {
    val r = g.rOuter
    val crownX = g.center.x + r * 1.04f
    val crownY = g.center.y - r * 0.04f

    val bodyW = r * 0.10f
    val bodyH = r * 0.24f
    val bodyTopLeft = Offset(crownX - bodyW / 2f, crownY - bodyH / 2f)
    drawRect(color = DialPalette.SteelLight, topLeft = bodyTopLeft, size = Size(bodyW, bodyH))
    val stripes = 9
    for (i in 1 until stripes) {
        val x = bodyTopLeft.x + bodyW * i / stripes
        drawLine(color = DialPalette.SteelGroove,
            start = Offset(x, bodyTopLeft.y + bodyH * 0.06f),
            end = Offset(x, bodyTopLeft.y + bodyH * 0.94f),
            strokeWidth = 1.2f)
    }
    drawCircle(color = DialPalette.SteelLight, radius = r * 0.060f,
        center = Offset(crownX + bodyW * 0.5f, crownY))
    drawCircle(color = DialPalette.SteelMid, radius = r * 0.060f,
        center = Offset(crownX + bodyW * 0.5f, crownY), style = Stroke(width = 0.8f))

    fun pusher(centerY: Float) {
        val capW = r * 0.07f
        val capH = r * 0.05f
        val shaftW = r * 0.04f
        val shaftH = r * 0.07f
        drawRect(color = DialPalette.SteelLight,
            topLeft = Offset(crownX - capW / 2f + r * 0.015f, centerY - capH / 2f),
            size = Size(capW, capH))
        drawRect(color = DialPalette.SteelMid,
            topLeft = Offset(crownX - capW / 2f + r * 0.015f, centerY - capH / 2f),
            size = Size(capW, capH), style = Stroke(width = 0.6f))
        drawRect(color = DialPalette.SteelMid,
            topLeft = Offset(crownX - shaftW / 2f - r * 0.030f, centerY - shaftH / 2f),
            size = Size(shaftW, shaftH))
    }
    pusher(g.center.y - r * 0.36f)
    pusher(g.center.y + r * 0.30f)
}

// =============================================================== text + triangle helpers

private fun DrawScope.drawScaleNumeralUpright(
    measurer: TextMeasurer,
    text: String,
    angleDegFromTop: Float,
    radius: Float,
    center: Offset,
    color: Color,
    sizeSp: androidx.compose.ui.unit.TextUnit,
    bold: Boolean = false
) {
    val rad = angleDegFromTop * PI / 180.0
    val x = center.x + (radius * cos(rad)).toFloat()
    val y = center.y + (radius * sin(rad)).toFloat()
    val l = measurer.measure(
        androidx.compose.ui.text.AnnotatedString(text),
        TextStyle(color = color, fontSize = sizeSp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    )
    val rot = angleDegFromTop + 90f
    rotate(rot, pivot = Offset(x, y)) {
        drawText(textLayoutResult = l,
            topLeft = Offset(x - l.size.width / 2f, y - l.size.height / 2f))
    }
}

private fun DrawScope.drawTriangleAtAngle(
    center: Offset, angleDeg: Float, radius: Float, size: Float,
    color: Color, inward: Boolean
) {
    val rad = angleDeg * PI / 180.0
    val tipR = if (inward) radius - size else radius + size
    val baseR = radius
    val perpRad = rad + PI / 2
    val tipX = center.x + (tipR * cos(rad)).toFloat()
    val tipY = center.y + (tipR * sin(rad)).toFloat()
    val baseCx = center.x + (baseR * cos(rad)).toFloat()
    val baseCy = center.y + (baseR * sin(rad)).toFloat()
    val px = (size * 0.6f * cos(perpRad)).toFloat()
    val py = (size * 0.6f * sin(perpRad)).toFloat()
    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(baseCx + px, baseCy + py)
        lineTo(baseCx - px, baseCy - py)
        close()
    }
    drawPath(path, color = color)
}
