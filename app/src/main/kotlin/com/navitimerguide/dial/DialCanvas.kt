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
 * The full watch face, structured as three stacked layers:
 *
 *   1. [StaticDial]      — dial background, brand marks, sub-dial faces,
 *                          inner fixed slide-rule scale, hour indices,
 *                          crown + pushers. Drawn once; Compose skips
 *                          recomposition when inputs (size + measurer)
 *                          don't change.
 *   2. [RotatingBezel]   — outer rotating bezel scale, drawn once into
 *                          its own graphics layer and rotated via
 *                          `graphicsLayer { rotationZ = … }` (no inner
 *                          redraw on rotation change).
 *   3. [LiveHandsLayer]  — hour, minute, central red chronograph hand,
 *                          and small-seconds sub-dial hand. Driven by
 *                          a 4 Hz tick (smooth enough for visible
 *                          movement, light on battery).
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
        // Black ring under the rotating bezel
        drawCircle(color = Color(0xFF050505), radius = g.rChapterOuter * 1.005f, center = g.center,
            style = Stroke(width = g.rOuter * 0.012f))
        drawFixedChapterRing(g, measurer)
        drawDialBackground(g)
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
            delay(250L)  // 4 Hz tick — smooth enough, easy on the battery
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

// =============================================================== draw helpers

private fun DrawScope.drawCoinEdgeBaseplate(g: DialGeom) {
    drawCircle(color = Color(0xFF101010), radius = g.rOuter, center = g.center)
    val teeth = 60
    for (i in 0 until teeth) {
        val angle = i * (360.0 / teeth)
        val rad = angle * PI / 180.0
        val rTip = g.rOuter * 1.0f
        val rBase = g.rOuter * 0.965f
        val perpRad = rad + PI / 2
        val halfWidth = g.rOuter * 0.012f
        val tipX = g.center.x + (rTip * cos(rad)).toFloat()
        val tipY = g.center.y + (rTip * sin(rad)).toFloat()
        val baseX = g.center.x + (rBase * cos(rad)).toFloat()
        val baseY = g.center.y + (rBase * sin(rad)).toFloat()
        val px = (halfWidth * cos(perpRad)).toFloat()
        val py = (halfWidth * sin(perpRad)).toFloat()
        drawLine(
            color = DialPalette.CrownSteel.copy(alpha = 0.9f),
            start = Offset(baseX + px, baseY + py),
            end = Offset(tipX + px, tipY + py),
            strokeWidth = 1.2f
        )
        drawLine(
            color = DialPalette.CrownSteel.copy(alpha = 0.9f),
            start = Offset(baseX - px, baseY - py),
            end = Offset(tipX - px, tipY - py),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawRotatingBezelScale(g: DialGeom, measurer: TextMeasurer) {
    val ringMid = (g.rBezelOuter + g.rBezelInner) / 2f
    val ringWidth = g.rBezelOuter - g.rBezelInner

    // The rotating bezel ring itself (no need for graphicsLayer to draw — the
    // PARENT applies rotationZ).
    drawCircle(color = Color(0xFF1B1B1B), radius = g.rBezelOuter, center = g.center)
    drawCircle(color = Color(0xFF000000), radius = g.rBezelInner, center = g.center)

    // Numerals 10..95 step 1 (every numeral as a tick; multiples of 5 labelled)
    for (v in 10..95) {
        val angle = DialMath.valueToAngle(v.toDouble())
        val rad = (angle - 90.0) * PI / 180.0
        val isMajor = v % 5 == 0
        val tickInner = ringMid + ringWidth * (if (isMajor) -0.42f else -0.32f)
        val tickOuter = ringMid + ringWidth * 0.42f
        val sx = g.center.x + (tickInner * cos(rad)).toFloat()
        val sy = g.center.y + (tickInner * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuter * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuter * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (isMajor) 0.95f else 0.6f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) 1.6f else 0.9f
        )
        if (isMajor) {
            val labelR = ringMid + ringWidth * 0.05f
            drawScaleNumeralUpright(
                measurer = measurer,
                text = v.toString(),
                angleDegFromTop = (angle - 90).toFloat(),
                radius = labelR,
                center = g.center,
                color = if (v == 60 || v == 10 || v == 36) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * 0.06f / density).sp
            )
        }
    }
    // Marker triangles on outer scale at red 60 / 10 / 36
    listOf(10.0, 36.0, 60.0).forEach { v ->
        val angle = DialMath.valueToAngle(v) - 90.0
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

private fun DrawScope.drawFixedChapterRing(g: DialGeom, measurer: TextMeasurer) {
    val midR = (g.rChapterOuter + g.rChapterInner) / 2f
    val width = g.rChapterOuter - g.rChapterInner

    drawCircle(color = Color(0xFF050505), radius = g.rChapterOuter, center = g.center)

    for (v in 10..95) {
        val angle = DialMath.valueToAngle(v.toDouble()) - 90.0
        val rad = angle * PI / 180.0
        val isMajor = v % 5 == 0
        val tickInner = midR - width * 0.42f
        val tickOuter = midR + width * 0.42f
        val sx = g.center.x + (tickInner * cos(rad)).toFloat()
        val sy = g.center.y + (tickInner * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuter * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuter * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (isMajor) 0.9f else 0.45f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) 1.4f else 0.7f
        )
        if (isMajor) {
            val isRed = (v == 60 || v == 10 || v == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = v.toString(),
                angleDegFromTop = angle.toFloat(),
                radius = midR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * 0.05f / density).sp
            )
        }
    }

    Markers.all.filter { it.side == ScaleSide.INNER }.forEach { m ->
        val angle = DialMath.valueToAngle(m.scaleValue) - 90.0
        if (m.style == MarkerStyle.TRIANGLE_OUTWARD) {
            drawTriangleAtAngle(
                center = g.center,
                angleDeg = angle.toFloat(),
                radius = midR + width * 0.40f,
                size = width * 0.16f,
                color = DialPalette.Red,
                inward = false
            )
        }
        m.text?.takeIf { m.style != MarkerStyle.RED_NUMERAL }?.let { txt ->
            drawScaleNumeralUpright(
                measurer = measurer,
                text = txt,
                angleDegFromTop = angle.toFloat(),
                radius = midR - width * 0.32f,
                center = g.center,
                color = DialPalette.Red,
                sizeSp = (g.rOuter * 0.040f / density).sp,
                bold = true
            )
        }
    }
}

private fun DrawScope.drawDialBackground(g: DialGeom) {
    val brush = Brush.radialGradient(
        colors = listOf(
            DialPalette.DialGreenInner,
            Color(0xFF073822),
            DialPalette.DialGreenOuter
        ),
        center = g.center,
        radius = g.rDial
    )
    drawCircle(brush = brush, radius = g.rDial, center = g.center)
}

private fun DrawScope.drawBrandMarks(g: DialGeom, measurer: TextMeasurer) {
    val anchorY = g.center.y - g.rDial * 0.32f
    val anchorScale = g.rDial * 0.10f
    drawAnchor(Offset(g.center.x, anchorY), anchorScale)

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
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerTopLeft: Offset
) {
    val l = measurer.measure(androidx.compose.ui.text.AnnotatedString(text), style)
    drawText(textLayoutResult = l,
        topLeft = Offset(centerTopLeft.x - l.size.width / 2f, centerTopLeft.y))
}

private fun DrawScope.drawAnchor(c: Offset, scale: Float) {
    val color = DialPalette.Numeral.copy(alpha = 0.95f)
    drawArc(color = color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(c.x - scale * 1.4f, c.y - scale * 0.7f),
        size = Size(scale * 2.8f, scale * 1.4f), style = Stroke(width = scale * 0.18f))
    drawLine(color = color, start = Offset(c.x, c.y - scale * 0.2f), end = Offset(c.x, c.y + scale * 0.9f),
        strokeWidth = scale * 0.18f)
    drawLine(color = color, start = Offset(c.x - scale * 0.55f, c.y + scale * 0.1f),
        end = Offset(c.x + scale * 0.55f, c.y + scale * 0.1f), strokeWidth = scale * 0.14f)
    drawArc(color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(c.x - scale * 0.7f, c.y + scale * 0.5f),
        size = Size(scale * 1.4f, scale * 0.7f), style = Stroke(width = scale * 0.16f))
}

// ----- sub-dial faces (no live hand here — that's drawn in LiveHandsLayer)

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
    // Date window (uses today's day-of-month, evaluated at draw-time)
    val now = currentLocalDateTime()
    val dateBoxW = subR * 0.65f
    val dateBoxH = subR * 0.32f
    val dateTopLeft = Offset(hrCenter.x - dateBoxW / 2f, hrCenter.y + subR * 0.25f)
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
    // Frozen 30-min and 12-hr counter hands (chronograph idle)
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
        drawLine(color = DialPalette.SubdialTick.copy(alpha = if (isMajor) 0.9f else 0.5f),
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
    val angle = (s * 6.0).toFloat()  // 360/60
    drawSubDialHand(secondsCenter, subR * 0.85f, angle, DialPalette.Hand, subR * 0.04f)
}

private fun DrawScope.drawDialHourIndices(g: DialGeom) {
    val skip = setOf(3, 6, 9)
    for (h in 0 until 12) {
        if (h in skip) continue
        val angle = h * 30.0 - 90.0
        val rad = angle * PI / 180.0
        val rIn = g.rDial * 0.62f
        val rOut = g.rDial * 0.74f
        val sx = g.center.x + (rIn * cos(rad)).toFloat()
        val sy = g.center.y + (rIn * sin(rad)).toFloat()
        val ex = g.center.x + (rOut * cos(rad)).toFloat()
        val ey = g.center.y + (rOut * sin(rad)).toFloat()
        drawLine(color = DialPalette.Hand, start = Offset(sx, sy), end = Offset(ex, ey),
            strokeWidth = g.rDial * 0.024f)
    }
}

private fun DrawScope.drawTimeHands(g: DialGeom, now: LocalDateTime) {
    val s = now.second + now.nanosecond / 1e9
    val mFull = now.minute + s / 60.0
    val hFull = (now.hour % 12) + mFull / 60.0
    val hourAngle = (hFull * 30.0).toFloat()
    val minAngle = (mFull * 6.0).toFloat()

    drawHandShape(g.center, hourAngle, g.rDial * 0.42f, g.rDial * 0.045f, DialPalette.Hand)
    drawHandShape(g.center, minAngle, g.rDial * 0.62f, g.rDial * 0.035f, DialPalette.Hand)
    // Central red chronograph second hand — frozen at 12 (chrono idle)
    drawLine(color = DialPalette.SecondHand, start = g.center,
        end = Offset(g.center.x, g.center.y - g.rDial * 0.66f),
        strokeWidth = g.rDial * 0.012f)
    drawLine(color = DialPalette.SecondHand, start = g.center,
        end = Offset(g.center.x, g.center.y + g.rDial * 0.10f),
        strokeWidth = g.rDial * 0.018f)
    drawCircle(color = DialPalette.Hand, radius = g.rDial * 0.025f, center = g.center)
    drawCircle(color = DialPalette.SecondHand, radius = g.rDial * 0.012f, center = g.center)
}

private fun DrawScope.drawHandShape(
    center: Offset, angleDeg: Float, length: Float, baseWidth: Float, color: Color
) {
    val rad = (angleDeg - 90.0) * PI / 180.0
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val tipX = center.x + length * cosA
    val tipY = center.y + length * sinA
    val baseLeftX = center.x + (-baseWidth) * (-sinA)
    val baseLeftY = center.y + (-baseWidth) * cosA
    val baseRightX = center.x + (baseWidth) * (-sinA)
    val baseRightY = center.y + (baseWidth) * cosA
    val path = Path().apply {
        moveTo(baseLeftX, baseLeftY)
        lineTo(tipX, tipY)
        lineTo(baseRightX, baseRightY)
        close()
    }
    drawPath(path = path, color = color)
    drawPath(path = path, color = Color(0xFF1A1A1A), style = Stroke(width = length * 0.012f))
}

private fun DrawScope.drawCrownAndPushers(g: DialGeom) {
    val crownX = g.center.x + g.rOuter * 1.04f
    val crownY = g.center.y - g.rOuter * 0.04f
    drawRect(color = DialPalette.CrownSteel,
        topLeft = Offset(crownX - g.rOuter * 0.06f, crownY - g.rOuter * 0.10f),
        size = Size(g.rOuter * 0.08f, g.rOuter * 0.20f))
    drawCircle(color = DialPalette.CrownSteel, radius = g.rOuter * 0.05f,
        center = Offset(crownX + g.rOuter * 0.02f, crownY))
    drawRect(color = DialPalette.CrownSteel,
        topLeft = Offset(crownX - g.rOuter * 0.05f, crownY - g.rOuter * 0.30f),
        size = Size(g.rOuter * 0.06f, g.rOuter * 0.10f))
    drawRect(color = DialPalette.CrownSteel,
        topLeft = Offset(crownX - g.rOuter * 0.05f, crownY + g.rOuter * 0.20f),
        size = Size(g.rOuter * 0.06f, g.rOuter * 0.10f))
}

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
