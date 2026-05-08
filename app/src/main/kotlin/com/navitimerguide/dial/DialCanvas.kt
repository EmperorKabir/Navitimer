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
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The full watch face, three layered Canvases (static / rotating / live).
 *
 * Layer 1 [StaticDial]   — chrome ring, bezel insert recess, fixed
 *                           rehaut scale, sunburst dial, brand marks,
 *                           sub-dial guilloché faces, applied indices,
 *                           crown and pushers.
 * Layer 2 [RotatingBezel] — outer rotating slide-rule scale; rotated
 *                           via `graphicsLayer { rotationZ }` so we
 *                           don't repaint on every rotation change.
 * Layer 3 [LiveHands]     — hour, minute, central red chronograph hand,
 *                           and small-seconds sub-dial hand at 9
 *                           o'clock. Driven by a 4 Hz tick.
 */
@Composable
fun WatchDial(
    bezelRotationDegrees: Double,
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
        LiveHandsLayer(modifier = Modifier.fillMaxSize())
    }
}

// =============================================================== StaticDial

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

// =============================================================== RotatingBezel

@Composable
private fun RotatingBezel(
    measurer: TextMeasurer,
    rotationDegrees: Double,
    modifier: Modifier
) {
    Canvas(
        modifier = modifier.graphicsLayer { rotationZ = rotationDegrees.toFloat() }
    ) {
        val g = geom()
        drawRotatingBezelScale(g, measurer)
    }
}

// =============================================================== LiveHandsLayer

@Composable
private fun LiveHandsLayer(modifier: Modifier) {
    val nowState: State<LocalDateTime> = produceState(initialValue = currentLocalDateTime()) {
        while (true) {
            value = currentLocalDateTime()
            delay(250L)
        }
    }
    val now = nowState.value
    Canvas(modifier = modifier) {
        val g = geom()
        drawSubDialSecondHand(g, now)
        drawTimeHands(g, now)
    }
}

private fun currentLocalDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/**
 * Numerals to print on each slide-rule scale, matching the real Navitimer:
 * every integer in 10..20, then 22, 24, 25, then every 5 from 30..95.
 * Other integers in 10..99 still get a tick (just no numeral).
 */
private val LABELED_NUMERALS: Set<Int> = (10..20).toSet() +
    setOf(22, 24, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95)

// =============================================================== geometry

private data class DialGeom(
    val w: Float, val h: Float,
    val cx: Float, val cy: Float,
    val center: Offset,
    val rOuter: Float,
    val rBezelOuter: Float,
    val rBezelInner: Float,
    val rChapterOuter: Float,
    val rChapterInner: Float,
    val rDial: Float
)

private fun DrawScope.geom(): DialGeom {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val rOuter = minOf(w, h) / 2f
    val rBezelOuter = rOuter * 0.99f
    val rBezelInner = rOuter * 0.86f
    val rChapterOuter = rOuter * 0.84f
    val rChapterInner = rOuter * 0.71f
    val rDial = rChapterInner
    return DialGeom(w, h, cx, cy, Offset(cx, cy), rOuter, rBezelOuter, rBezelInner, rChapterOuter, rChapterInner, rDial)
}

// =============================================================== chrome teeth

private fun DrawScope.drawCoinEdgeBaseplate(g: DialGeom) {
    val teeth = 90
    val rTip = g.rOuter
    val rBase = g.rOuter * 0.94f
    // Substrate
    drawCircle(color = Color(0xFF0C0C0E), radius = g.rOuter, center = g.center)

    for (i in 0 until teeth) {
        val angle = i * (360.0 / teeth)
        val rad = angle * PI / 180.0
        val perpRad = rad + PI / 2
        val toothHalf = g.rOuter * 0.014f
        val toothCx = g.center.x + ((rTip + rBase) / 2 * cos(rad)).toFloat()
        val toothCy = g.center.y + ((rTip + rBase) / 2 * sin(rad)).toFloat()
        val tipX = g.center.x + (rTip * cos(rad)).toFloat()
        val tipY = g.center.y + (rTip * sin(rad)).toFloat()
        val baseX = g.center.x + (rBase * cos(rad)).toFloat()
        val baseY = g.center.y + (rBase * sin(rad)).toFloat()
        val px = (toothHalf * cos(perpRad)).toFloat()
        val py = (toothHalf * sin(perpRad)).toFloat()

        // Groove (dark) on the leading side
        drawLine(
            color = DialPalette.SteelGroove,
            start = Offset(baseX + px * 1.3f, baseY + py * 1.3f),
            end = Offset(tipX + px * 1.3f, tipY + py * 1.3f),
            strokeWidth = toothHalf * 0.65f
        )
        // Bright highlight (centre of tooth)
        drawLine(
            color = DialPalette.SteelLight,
            start = Offset(baseX, baseY),
            end = Offset(tipX, tipY),
            strokeWidth = toothHalf * 0.75f
        )
        // Mid-grey shadow on the trailing side
        drawLine(
            color = DialPalette.SteelMid,
            start = Offset(baseX - px * 0.6f, baseY - py * 0.6f),
            end = Offset(tipX - px * 0.6f, tipY - py * 0.6f),
            strokeWidth = toothHalf * 0.55f
        )
    }

    // Outer chrome highlight ring
    drawCircle(color = DialPalette.SteelLight, radius = g.rOuter, center = g.center,
        style = Stroke(width = 1.2f))
    // Inner shadow at the joint between bezel teeth and the case
    drawCircle(color = DialPalette.BezelEdgeShadow, radius = rBase, center = g.center,
        style = Stroke(width = g.rOuter * 0.008f))
}

// =============================================================== bezel insert recess

private fun DrawScope.drawBezelInsertRecess(g: DialGeom) {
    drawCircle(color = DialPalette.BezelInsertBlack, radius = g.rBezelOuter, center = g.center)
    drawCircle(color = Color(0xFF1B1B1B), radius = g.rBezelOuter, center = g.center,
        style = Stroke(width = g.rOuter * 0.006f))
    drawCircle(color = DialPalette.BezelEdgeShadow, radius = g.rBezelInner, center = g.center,
        style = Stroke(width = g.rOuter * 0.012f))
}

// =============================================================== rotating bezel scale

private fun DrawScope.drawRotatingBezelScale(g: DialGeom, measurer: TextMeasurer) {
    val ringMid = (g.rBezelOuter + g.rBezelInner) / 2f
    val ringWidth = g.rBezelOuter - g.rBezelInner

    // Numerals + ticks: every integer 10..99
    for (v in 10..99) {
        val angle = DialMath.drawAngleDeg(v.toDouble())
        val rad = angle * PI / 180.0
        val isMajor = v % 5 == 0
        val tickInner = ringMid + ringWidth * (if (isMajor) -0.42f else -0.28f)
        val tickOuter = ringMid + ringWidth * 0.42f
        val sx = g.center.x + (tickInner * cos(rad)).toFloat()
        val sy = g.center.y + (tickInner * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuter * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuter * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (isMajor) 0.95f else 0.55f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) 1.6f else 0.7f
        )
        if (v in LABELED_NUMERALS) {
            val labelR = ringMid + ringWidth * 0.05f
            val isRed = (v == 60 || v == 10 || v == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = v.toString(),
                angleDegFromTop = angle.toFloat(),
                radius = labelR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * (if (v % 5 == 0) 0.058f else 0.040f) / density).sp
            )
        }
    }
    // Red triangle indices on outer bezel at 10 / 36 / 60
    listOf(10.0, 36.0, 60.0).forEach { v ->
        val angle = DialMath.drawAngleDeg(v)
        drawTriangleAtAngle(
            center = g.center,
            angleDeg = angle.toFloat(),
            radius = ringMid + ringWidth * 0.40f,
            size = ringWidth * 0.18f,
            color = DialPalette.Red,
            inward = true
        )
    }
}

// =============================================================== fixed chapter ring

private fun DrawScope.drawFixedChapterRing(g: DialGeom, measurer: TextMeasurer) {
    val midR = (g.rChapterOuter + g.rChapterInner) / 2f
    val width = g.rChapterOuter - g.rChapterInner

    drawCircle(color = Color(0xFF050505), radius = g.rChapterOuter, center = g.center)
    // Subtle inset shadow at outer edge — suggests the angled rehaut step
    drawCircle(color = Color(0xFF1F1F1F), radius = g.rChapterOuter, center = g.center,
        style = Stroke(width = width * 0.05f))

    // Numerals + ticks: every integer 10..99
    for (v in 10..99) {
        val angle = DialMath.drawAngleDeg(v.toDouble())
        val rad = angle * PI / 180.0
        val isMajor = v % 5 == 0
        val tickInner = midR - width * 0.42f
        val tickOuter = midR + width * 0.42f
        val sx = g.center.x + (tickInner * cos(rad)).toFloat()
        val sy = g.center.y + (tickInner * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuter * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuter * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (isMajor) 0.9f else 0.40f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) 1.4f else 0.6f
        )
        if (v in LABELED_NUMERALS) {
            val isRed = (v == 60 || v == 10 || v == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = v.toString(),
                angleDegFromTop = angle.toFloat(),
                radius = midR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * (if (v % 5 == 0) 0.048f else 0.034f) / density).sp
            )
        }
    }

    // Markers on inner ring: KM (text), STAT (triangle+text), NAUT (triangle+text), MPH (text under red 60)
    val markerLabelR = midR - width * 0.32f
    Markers.all.filter { it.side == ScaleSide.INNER && it.style != MarkerStyle.RED_NUMERAL }
        .forEach { m ->
        val angle = DialMath.drawAngleDeg(m.scaleValue)
        if (m.style == MarkerStyle.TRIANGLE_OUTWARD) {
            drawTriangleAtAngle(
                center = g.center,
                angleDeg = angle.toFloat(),
                radius = midR + width * 0.40f,
                size = width * 0.18f,
                color = DialPalette.Red,
                inward = false
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

// =============================================================== dial bg + sunburst + highlight

private fun DrawScope.drawDialBackground(g: DialGeom) {
    val brush = Brush.radialGradient(
        colors = listOf(
            DialPalette.DialGreenInner,
            DialPalette.DialGreenSpokeDark,
            DialPalette.DialGreenOuter
        ),
        center = g.center,
        radius = g.rDial
    )
    drawCircle(brush = brush, radius = g.rDial, center = g.center)
}

private fun DrawScope.drawSunburstOverlay(g: DialGeom) {
    // 240 thin radial spokes, alternating slightly lighter / darker green.
    // Drawn as thin lines from a small inner radius (skip dial centre) to
    // the outer edge of the dial; alpha tapers via two stroke draws.
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
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = 1.0f
        )
    }
}

private fun DrawScope.drawDialHighlight(g: DialGeom) {
    // Soft off-centre white glow (top-left), like the photo's flash highlight
    val glow = Brush.radialGradient(
        colors = listOf(
            Color(0x33FFFFFF),
            Color(0x11FFFFFF),
            Color(0x00FFFFFF)
        ),
        center = Offset(g.center.x - g.rDial * 0.45f, g.center.y - g.rDial * 0.55f),
        radius = g.rDial * 0.85f
    )
    drawCircle(brush = glow, radius = g.rDial, center = g.center)
}

// =============================================================== brand marks + anchor

private fun DrawScope.drawBrandMarks(g: DialGeom, measurer: TextMeasurer) {
    val anchorY = g.center.y - g.rDial * 0.34f
    val anchorScale = g.rDial * 0.10f
    drawWingedAnchor(Offset(g.center.x, anchorY), anchorScale)

    drawCenteredText(measurer, "BREITLING",
        TextStyle(color = DialPalette.Numeral, fontSize = (g.rDial * 0.10f / density).sp,
            fontWeight = FontWeight.Bold, letterSpacing = (g.rDial * 0.005f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.21f))

    drawCenteredText(measurer, "1884",
        TextStyle(color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (g.rDial * 0.05f / density).sp, fontWeight = FontWeight.Medium),
        Offset(g.center.x, g.center.y - g.rDial * 0.115f))

    drawCenteredText(measurer, "NAVITIMER",
        TextStyle(color = DialPalette.Numeral, fontSize = (g.rDial * 0.062f / density).sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (g.rDial * 0.004f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.05f))

    drawCenteredText(measurer, "SWISS  MADE",
        TextStyle(color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (g.rDial * 0.035f / density).sp, fontWeight = FontWeight.Medium,
            letterSpacing = (g.rDial * 0.004f / density).sp),
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
    // Three stacked wing arcs each side, progressively smaller sweep
    val wingSweeps = listOf(140f, 110f, 80f)
    val wingRadii = listOf(1.4f, 1.15f, 0.9f)
    val wingStartAngles = listOf(200f, 210f, 220f)  // top arc
    for (i in wingSweeps.indices) {
        val r = scale * wingRadii[i]
        drawArc(
            color = color,
            startAngle = wingStartAngles[i],
            sweepAngle = wingSweeps[i],
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r * 0.55f),
            size = Size(r * 2, r * 1.1f),
            style = Stroke(width = scale * (0.10f - i * 0.018f))
        )
    }
    // Anchor: ring at top
    drawCircle(color = color, center = Offset(c.x, c.y - scale * 0.35f), radius = scale * 0.13f,
        style = Stroke(width = scale * 0.08f))
    // Stem
    drawLine(color = color, start = Offset(c.x, c.y - scale * 0.20f),
        end = Offset(c.x, c.y + scale * 1.0f), strokeWidth = scale * 0.10f)
    // Crossbar
    drawLine(color = color, start = Offset(c.x - scale * 0.65f, c.y + scale * 0.05f),
        end = Offset(c.x + scale * 0.65f, c.y + scale * 0.05f), strokeWidth = scale * 0.09f)
    // Left fluke
    drawArc(color = color, startAngle = 70f, sweepAngle = 110f, useCenter = false,
        topLeft = Offset(c.x - scale * 0.70f, c.y + scale * 0.50f),
        size = Size(scale * 0.70f, scale * 0.65f),
        style = Stroke(width = scale * 0.10f))
    // Right fluke
    drawArc(color = color, startAngle = 0f, sweepAngle = 110f, useCenter = false,
        topLeft = Offset(c.x, c.y + scale * 0.50f),
        size = Size(scale * 0.70f, scale * 0.65f),
        style = Stroke(width = scale * 0.10f))
    // Centre rivet
    drawCircle(color = color, center = Offset(c.x, c.y + scale * 0.05f), radius = scale * 0.08f)
}

// =============================================================== sub-dials (faces only)

private fun DrawScope.drawSubDialFaces(g: DialGeom, measurer: TextMeasurer) {
    val subR = g.rDial * 0.22f
    val offset = g.rDial * 0.42f
    drawSubDialFace(Offset(g.center.x - offset, g.center.y), subR, ticks = 60, majorEvery = 5,
        ringNumbers = listOf(20 to "20", 40 to "40"), measurer = measurer)
    drawSubDialFace(Offset(g.center.x + offset, g.center.y), subR, ticks = 30, majorEvery = 5,
        ringNumbers = listOf(10 to "10", 20 to "20", 30 to "30"), measurer = measurer)
    val hrCenter = Offset(g.center.x, g.center.y + offset)
    drawSubDialFace(hrCenter, subR, ticks = 12, majorEvery = 3,
        ringNumbers = listOf(3 to "3", 6 to "6", 9 to "9", 12 to "12"), measurer = measurer)
    // Date window
    val now = currentLocalDateTime()
    val dateBoxW = subR * 0.65f
    val dateBoxH = subR * 0.32f
    val dateTopLeft = Offset(hrCenter.x - dateBoxW / 2f, hrCenter.y + subR * 0.25f)
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
    // Frozen 30-min and 12-hr counter hands (chrono idle)
    val staticMin = Offset(g.center.x + offset, g.center.y)
    drawSubDialHand(staticMin, subR * 0.8f, 0f, DialPalette.Hand, subR * 0.04f)
    drawSubDialHand(hrCenter, subR * 0.78f, 0f, DialPalette.Hand, subR * 0.04f)
}

private fun DrawScope.drawSubDialFace(
    center: Offset, radius: Float, ticks: Int, majorEvery: Int,
    ringNumbers: List<Pair<Int, String>>, measurer: TextMeasurer
) {
    drawCircle(color = DialPalette.SubdialBlack, radius = radius, center = center)
    drawCircle(color = Color(0xFF1A1A1A), radius = radius, center = center,
        style = Stroke(width = radius * 0.04f))

    // Azure / guilloché — concentric thin rings
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

private fun DrawScope.drawSubDialSecondHand(g: DialGeom, now: LocalDateTime) {
    val subR = g.rDial * 0.22f
    val offset = g.rDial * 0.42f
    val secondsCenter = Offset(g.center.x - offset, g.center.y)
    val s = now.second + now.nanosecond / 1e9
    val angle = (s * 6.0).toFloat()
    drawSubDialHand(secondsCenter, subR * 0.85f, angle, DialPalette.Hand, subR * 0.04f)
}

// =============================================================== applied baton hour indices

private fun DrawScope.drawDialHourIndices(g: DialGeom) {
    val skip = setOf(3, 6, 9)
    for (h in 0 until 12) {
        if (h in skip) continue
        val angle = h * 30.0 - 90.0
        val rad = angle * PI / 180.0
        val rIn = g.rDial * 0.62f
        val rOut = g.rDial * 0.74f
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val cx = g.center.x + ((rIn + rOut) / 2 * cos(rad)).toFloat()
        val cy = g.center.y + ((rIn + rOut) / 2 * sin(rad)).toFloat()
        // Frame and lume widths (perpendicular to radial direction)
        val frameW = g.rDial * 0.030f
        val lumeW = g.rDial * 0.020f
        val length = rOut - rIn
        // Build a parallelogram path along radial direction with given perp width
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
        // Frame (outer chrome rectangle)
        drawPath(path = batonPath(frameW), color = DialPalette.HandFrame)
        // Lume (cream inset rectangle)
        drawPath(path = batonPath(lumeW), color = DialPalette.Lume)
        // Frame outline
        drawPath(path = batonPath(frameW), color = Color(0xFF20242A),
            style = Stroke(width = 0.8f))
    }
}

// =============================================================== hands

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
    // Central red chronograph hand — frozen at 12 (chrono idle)
    drawLine(color = DialPalette.SecondHand, start = g.center,
        end = Offset(g.center.x, g.center.y - g.rDial * 0.66f),
        strokeWidth = g.rDial * 0.012f)
    drawLine(color = DialPalette.SecondHand, start = g.center,
        end = Offset(g.center.x, g.center.y + g.rDial * 0.10f),
        strokeWidth = g.rDial * 0.018f)
    // Hub: chrome boss + tiny red dot
    drawCircle(color = DialPalette.HandFrame, radius = g.rDial * 0.030f, center = g.center)
    drawCircle(color = DialPalette.Hand, radius = g.rDial * 0.025f, center = g.center)
    drawCircle(color = DialPalette.SecondHand, radius = g.rDial * 0.012f, center = g.center)
}

/**
 * A baton-shaped hand with chrome frame and lume centre stripe.
 * angleDeg = 0 points to 12 o'clock; clockwise positive.
 */
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
            // back base
            moveTo(backX + perpX, backY + perpY)
            lineTo(backX - perpX, backY - perpY)
            // along the hand to just before tip
            lineTo(tipPreX - perpX, tipPreY - perpY)
            // chamfer to tip
            lineTo(tipX, tipY)
            lineTo(tipPreX + perpX, tipPreY + perpY)
            close()
        }
    }
    drawPath(handPath(outerW), color = DialPalette.HandFrame)
    drawPath(handPath(innerW), color = DialPalette.Lume)
    drawPath(handPath(outerW), color = Color(0xFF1A1A1A), style = Stroke(width = 0.8f))
}

// =============================================================== crown + pushers

private fun DrawScope.drawCrownAndPushers(g: DialGeom) {
    val r = g.rOuter
    val crownX = g.center.x + r * 1.04f
    val crownY = g.center.y - r * 0.04f

    // Crown body (cylindrical, with reeded grip)
    val bodyW = r * 0.09f
    val bodyH = r * 0.22f
    val bodyTopLeft = Offset(crownX - bodyW / 2f, crownY - bodyH / 2f)
    drawRect(color = DialPalette.SteelLight, topLeft = bodyTopLeft, size = Size(bodyW, bodyH))
    // Vertical reeded stripes
    val stripes = 9
    for (i in 1 until stripes) {
        val x = bodyTopLeft.x + bodyW * i / stripes
        drawLine(color = DialPalette.SteelGroove,
            start = Offset(x, bodyTopLeft.y + bodyH * 0.05f),
            end = Offset(x, bodyTopLeft.y + bodyH * 0.95f),
            strokeWidth = 1.2f)
    }
    // Cap (the outer face)
    val capR = r * 0.06f
    drawCircle(color = DialPalette.SteelLight, radius = capR, center = Offset(crownX + bodyW * 0.45f, crownY))
    drawCircle(color = DialPalette.SteelMid, radius = capR, center = Offset(crownX + bodyW * 0.45f, crownY),
        style = Stroke(width = 0.8f))

    // Pushers (two)
    fun pusher(centerY: Float) {
        val capW = r * 0.07f
        val capH = r * 0.04f
        val shaftW = r * 0.04f
        val shaftH = r * 0.06f
        // Cap rectangle (outermost)
        drawRect(color = DialPalette.SteelLight,
            topLeft = Offset(crownX - capW / 2f + r * 0.01f, centerY - capH / 2f),
            size = Size(capW, capH))
        drawRect(color = DialPalette.SteelMid,
            topLeft = Offset(crownX - capW / 2f + r * 0.01f, centerY - capH / 2f),
            size = Size(capW, capH), style = Stroke(width = 0.6f))
        // Shaft (narrower, closer to case)
        drawRect(color = DialPalette.SteelMid,
            topLeft = Offset(crownX - shaftW / 2f - r * 0.025f, centerY - shaftH / 2f),
            size = Size(shaftW, shaftH))
    }
    pusher(g.center.y - r * 0.36f)
    pusher(g.center.y + r * 0.30f)
}

// =============================================================== text helpers

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
