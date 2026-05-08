package com.navitimerguide.dial

import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * The whole watch face. Pure drawing — receives bezel rotation as a state.
 * Live time-of-day hands tick from the system clock.
 */
@Composable
fun WatchDial(
    bezelRotationDegrees: Double,
    modifier: Modifier = Modifier
) {
    // Drive a frame-tick state so the hands animate.
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { now -> frameTick = now }
        }
    }

    val measurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
    ) {
        drawWatchFace(
            bezelRotationDegrees = bezelRotationDegrees,
            now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            frameNanos = frameTick,
            measurer = measurer
        )
    }
}

private fun DrawScope.drawWatchFace(
    bezelRotationDegrees: Double,
    now: LocalDateTime,
    frameNanos: Long,
    measurer: TextMeasurer
) {
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
    val center = Offset(cx, cy)

    // 1. Coin-edge bezel base ring
    drawCoinEdgeBezel(center, rBezelOuter, rBezelInner)

    // 2. The rotating bezel scale (numbers + STAT/NAUT/KM markers + red 60/10/36 indices)
    drawRotatingBezelScale(
        center = center,
        radiusOuter = rBezelOuter,
        radiusInner = rBezelInner,
        rotationDegrees = bezelRotationDegrees,
        measurer = measurer
    )

    // 3. Inner fixed chapter ring (the rehaut)
    drawFixedChapterRing(
        center = center,
        radiusOuter = rChapterOuter,
        radiusInner = rChapterInner,
        measurer = measurer
    )

    // 4. Green sunburst dial
    drawDialBackground(center, rDial)

    // 5. Brand wordmark + anchor (top-centre area)
    drawBrandMarks(center, rDial, measurer)

    // 6. Sub-dials
    drawSubDials(center, rDial, now, measurer)

    // 7. Indices on the dial (hour markers, NOT scale numerals)
    drawDialHourIndices(center, rDial)

    // 8. Live hour / minute hands + frozen central red chronograph hand
    drawHands(center, rDial, now)

    // 9. Crown + pushers (right edge)
    drawCrownAndPushers(center, rOuter)
}

// -------------------------------------------------------------------- 1
private fun DrawScope.drawCoinEdgeBezel(
    center: Offset,
    rOuter: Float,
    rInner: Float
) {
    // Black bezel base
    drawCircle(color = Color(0xFF101010), radius = rOuter, center = center)
    // Coin-edge teeth
    val teeth = 60
    for (i in 0 until teeth) {
        val angle = i * (360.0 / teeth)
        val rad = angle * PI / 180.0
        val rTip = rOuter * 1.0f
        val rBase = rOuter * 0.965f
        val perpRad = rad + PI / 2
        val halfWidth = rOuter * 0.012f
        val tipX = center.x + (rTip * cos(rad)).toFloat()
        val tipY = center.y + (rTip * sin(rad)).toFloat()
        val baseX = center.x + (rBase * cos(rad)).toFloat()
        val baseY = center.y + (rBase * sin(rad)).toFloat()
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
    // Inner shadow ring
    drawCircle(
        color = Color(0xFF050505),
        radius = rInner * 1.005f,
        center = center,
        style = Stroke(width = rOuter * 0.012f)
    )
}

// -------------------------------------------------------------------- 2
private fun DrawScope.drawRotatingBezelScale(
    center: Offset,
    radiusOuter: Float,
    radiusInner: Float,
    rotationDegrees: Double,
    measurer: TextMeasurer
) {
    // Solid white-ish band
    val ringMid = (radiusOuter + radiusInner) / 2f
    val ringWidth = radiusOuter - radiusInner

    // Background of the rotating ring (slightly darker than bezel coin)
    drawCircle(color = Color(0xFF1B1B1B), radius = radiusOuter, center = center)
    drawCircle(color = Color(0xFF000000), radius = radiusInner, center = center)

    rotate(degrees = rotationDegrees.toFloat() - 90f, pivot = center) {
        // Numerals 10..90 step 5 (and minor ticks for sub-divisions)
        for (v in 10..95) {
            val angle = DialMath.valueToAngle(v.toDouble())
            val rad = (angle - 90.0) * PI / 180.0  // -90° to put "10" at top
            val isMajor = v % 5 == 0
            val isLabel = v % 5 == 0
            val tickInner = ringMid + ringWidth * (if (isMajor) -0.42f else -0.32f)
            val tickOuter = ringMid + ringWidth * 0.42f
            val sx = center.x + (tickInner * cos(rad)).toFloat()
            val sy = center.y + (tickInner * sin(rad)).toFloat()
            val ex = center.x + (tickOuter * cos(rad)).toFloat()
            val ey = center.y + (tickOuter * sin(rad)).toFloat()
            drawLine(
                color = DialPalette.Numeral.copy(alpha = if (isMajor) 0.95f else 0.6f),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (isMajor) 1.6f else 0.9f
            )
            if (isLabel) {
                val labelR = ringMid + ringWidth * 0.05f
                drawScaleNumeralUpright(
                    measurer = measurer,
                    text = v.toString(),
                    angleDegFromTop = (angle - 90).toFloat(),
                    radius = labelR,
                    center = center,
                    color = if (v == 60 || v == 10 || v == 36) DialPalette.Red else DialPalette.Numeral,
                    sizeSp = (radiusOuter * 0.06f / density).sp
                )
            }
        }

        // Marker triangles on outer scale at red 60 / 10 / 36
        listOf(10.0, 36.0, 60.0).forEach { v ->
            val angle = DialMath.valueToAngle(v) - 90.0
            drawTriangleAtAngle(
                center = center,
                angleDeg = angle.toFloat(),
                radius = ringMid + ringWidth * 0.40f,
                size = ringWidth * 0.18f,
                color = DialPalette.Red,
                inward = true
            )
        }
    }
}

// -------------------------------------------------------------------- 3
private fun DrawScope.drawFixedChapterRing(
    center: Offset,
    radiusOuter: Float,
    radiusInner: Float,
    measurer: TextMeasurer
) {
    val midR = (radiusOuter + radiusInner) / 2f
    val width = radiusOuter - radiusInner

    drawCircle(color = Color(0xFF050505), radius = radiusOuter, center = center)

    // Numerals 10..95 step 5 on the inner FIXED scale
    for (v in 10..95) {
        val angle = DialMath.valueToAngle(v.toDouble()) - 90.0
        val rad = angle * PI / 180.0
        val isMajor = v % 5 == 0
        val tickInner = midR - width * 0.42f
        val tickOuter = midR + width * 0.42f
        val sx = center.x + (tickInner * cos(rad)).toFloat()
        val sy = center.y + (tickInner * sin(rad)).toFloat()
        val ex = center.x + (tickOuter * cos(rad)).toFloat()
        val ey = center.y + (tickOuter * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = if (isMajor) 0.9f else 0.45f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) 1.4f else 0.7f
        )
        if (isMajor) {
            val labelR = midR
            val isRed = (v == 60 || v == 10 || v == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = v.toString(),
                angleDegFromTop = angle.toFloat(),
                radius = labelR,
                center = center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (radiusOuter * 0.05f / density).sp
            )
        }
    }

    // Markers: KM / STAT / NAUT / MPH (text labels)
    Markers.all.filter { it.side == ScaleSide.INNER && it.style != MarkerStyle.RED_NUMERAL }
        .forEach { m ->
            val angle = DialMath.valueToAngle(m.scaleValue) - 90.0
            val labelR = midR - width * 0.32f
            // Triangle marker
            if (m.style != MarkerStyle.TEXT) {
                drawTriangleAtAngle(
                    center = center,
                    angleDeg = angle.toFloat(),
                    radius = midR + width * 0.40f,
                    size = width * 0.16f,
                    color = DialPalette.Red,
                    inward = false
                )
            }
            m.text?.let { txt ->
                drawScaleNumeralUpright(
                    measurer = measurer,
                    text = txt,
                    angleDegFromTop = angle.toFloat(),
                    radius = labelR,
                    center = center,
                    color = DialPalette.Red,
                    sizeSp = (radiusOuter * 0.040f / density).sp,
                    bold = true
                )
            }
        }
}

// -------------------------------------------------------------------- 4
private fun DrawScope.drawDialBackground(center: Offset, radius: Float) {
    val brush = Brush.radialGradient(
        colors = listOf(
            DialPalette.DialGreenInner,
            Color(0xFF073822),
            DialPalette.DialGreenOuter
        ),
        center = center,
        radius = radius
    )
    drawCircle(brush = brush, radius = radius, center = center)
}

// -------------------------------------------------------------------- 5
private fun DrawScope.drawBrandMarks(
    center: Offset,
    rDial: Float,
    measurer: TextMeasurer
) {
    // Anchor + wings: simple stylised glyph drawn with primitives
    val anchorY = center.y - rDial * 0.32f
    val anchorScale = rDial * 0.10f
    drawAnchor(Offset(center.x, anchorY), anchorScale)

    // BREITLING wordmark
    val br = measurer.measure(
        text = androidx.compose.ui.text.AnnotatedString("BREITLING"),
        style = TextStyle(
            color = DialPalette.Numeral,
            fontSize = (rDial * 0.10f / density).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (rDial * 0.005f / density).sp
        )
    )
    drawText(
        textLayoutResult = br,
        topLeft = Offset(center.x - br.size.width / 2f, center.y - rDial * 0.21f)
    )
    // 1884
    val year = measurer.measure(
        text = androidx.compose.ui.text.AnnotatedString("1884"),
        style = TextStyle(
            color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (rDial * 0.05f / density).sp,
            fontWeight = FontWeight.Medium
        )
    )
    drawText(
        textLayoutResult = year,
        topLeft = Offset(center.x - year.size.width / 2f, center.y - rDial * 0.115f)
    )
    // NAVITIMER
    val nv = measurer.measure(
        text = androidx.compose.ui.text.AnnotatedString("NAVITIMER"),
        style = TextStyle(
            color = DialPalette.Numeral,
            fontSize = (rDial * 0.062f / density).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (rDial * 0.004f / density).sp
        )
    )
    drawText(
        textLayoutResult = nv,
        topLeft = Offset(center.x - nv.size.width / 2f, center.y - rDial * 0.05f)
    )
    // SWISS  MADE at bottom
    val sm = measurer.measure(
        text = androidx.compose.ui.text.AnnotatedString("SWISS  MADE"),
        style = TextStyle(
            color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (rDial * 0.035f / density).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (rDial * 0.004f / density).sp
        )
    )
    drawText(
        textLayoutResult = sm,
        topLeft = Offset(center.x - sm.size.width / 2f, center.y + rDial * 0.83f)
    )
}

private fun DrawScope.drawAnchor(c: Offset, scale: Float) {
    val color = DialPalette.Numeral.copy(alpha = 0.95f)
    // Wings (simplified)
    val wingR = scale * 1.4f
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(c.x - wingR, c.y - wingR / 2),
        size = Size(wingR * 2, wingR),
        style = Stroke(width = scale * 0.18f)
    )
    // Stem
    drawLine(
        color = color,
        start = Offset(c.x, c.y - scale * 0.2f),
        end = Offset(c.x, c.y + scale * 0.9f),
        strokeWidth = scale * 0.18f
    )
    // Crossbar
    drawLine(
        color = color,
        start = Offset(c.x - scale * 0.55f, c.y + scale * 0.1f),
        end = Offset(c.x + scale * 0.55f, c.y + scale * 0.1f),
        strokeWidth = scale * 0.14f
    )
    // Flukes
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(c.x - scale * 0.7f, c.y + scale * 0.5f),
        size = Size(scale * 1.4f, scale * 0.7f),
        style = Stroke(width = scale * 0.16f)
    )
}

// -------------------------------------------------------------------- 6
private fun DrawScope.drawSubDials(
    center: Offset,
    rDial: Float,
    now: LocalDateTime,
    measurer: TextMeasurer
) {
    val subR = rDial * 0.22f
    val offset = rDial * 0.42f

    // 9 o'clock — small running seconds (LIVE)
    val secondsCenter = Offset(center.x - offset, center.y)
    drawSubDial(secondsCenter, subR, label = null, ticks = 60, majorEvery = 5, measurer = measurer, ringNumbers = listOf(20 to "20", 40 to "40"))
    val secondAngleDeg = (now.second + now.nanosecond / 1e9) * 6.0  // 360/60
    drawSubDialHand(secondsCenter, subR * 0.85f, secondAngleDeg.toFloat(), DialPalette.Hand, thickness = subR * 0.04f)

    // 3 o'clock — chronograph 30-min counter (frozen at 0)
    val minCenter = Offset(center.x + offset, center.y)
    drawSubDial(minCenter, subR, label = null, ticks = 30, majorEvery = 5, measurer = measurer, ringNumbers = listOf(10 to "10", 20 to "20", 30 to "30"))
    drawSubDialHand(minCenter, subR * 0.8f, 0f, DialPalette.Hand, thickness = subR * 0.04f)

    // 6 o'clock — chronograph 12-hr counter (frozen) + date window "17"
    val hrCenter = Offset(center.x, center.y + offset)
    drawSubDial(hrCenter, subR, label = null, ticks = 12, majorEvery = 3, measurer = measurer, ringNumbers = listOf(3 to "3", 6 to "6", 9 to "9", 12 to "12"))
    drawSubDialHand(hrCenter, subR * 0.78f, 0f, DialPalette.Hand, thickness = subR * 0.04f)
    // Date window
    val dateBoxW = subR * 0.65f
    val dateBoxH = subR * 0.32f
    val dateBoxTopLeft = Offset(hrCenter.x - dateBoxW / 2f, hrCenter.y + subR * 0.25f)
    drawRect(
        color = Color.White,
        topLeft = dateBoxTopLeft,
        size = Size(dateBoxW, dateBoxH)
    )
    val dateText = measurer.measure(
        text = androidx.compose.ui.text.AnnotatedString(now.dayOfMonth.toString()),
        style = TextStyle(
            color = Color.Black,
            fontSize = (subR * 0.4f / density).sp,
            fontWeight = FontWeight.SemiBold
        )
    )
    drawText(
        textLayoutResult = dateText,
        topLeft = Offset(
            dateBoxTopLeft.x + (dateBoxW - dateText.size.width) / 2f,
            dateBoxTopLeft.y + (dateBoxH - dateText.size.height) / 2f
        )
    )
}

private fun DrawScope.drawSubDial(
    center: Offset,
    radius: Float,
    label: String?,
    ticks: Int,
    majorEvery: Int,
    measurer: TextMeasurer,
    ringNumbers: List<Pair<Int, String>>
) {
    drawCircle(color = DialPalette.SubdialBlack, radius = radius, center = center)
    drawCircle(color = Color(0xFF1A1A1A), radius = radius, center = center, style = Stroke(width = radius * 0.04f))
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
        drawLine(
            color = DialPalette.SubdialTick.copy(alpha = if (isMajor) 0.9f else 0.5f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) radius * 0.025f else radius * 0.012f
        )
    }
    for ((tick, txt) in ringNumbers) {
        val angle = tick * (360.0 / ticks) - 90.0
        val rad = angle * PI / 180.0
        val rL = radius * 0.62f
        val tx = center.x + (rL * cos(rad)).toFloat()
        val ty = center.y + (rL * sin(rad)).toFloat()
        val l = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(txt),
            style = TextStyle(
                color = DialPalette.Numeral,
                fontSize = (radius * 0.22f / density).sp,
                fontWeight = FontWeight.Medium
            )
        )
        drawText(
            textLayoutResult = l,
            topLeft = Offset(tx - l.size.width / 2f, ty - l.size.height / 2f)
        )
    }
    drawCircle(color = DialPalette.Hand, radius = radius * 0.05f, center = center)
}

private fun DrawScope.drawSubDialHand(
    center: Offset,
    length: Float,
    angleDeg: Float,
    color: Color,
    thickness: Float
) {
    val rad = (angleDeg - 90.0) * PI / 180.0
    val ex = center.x + (length * cos(rad)).toFloat()
    val ey = center.y + (length * sin(rad)).toFloat()
    drawLine(color = color, start = center, end = Offset(ex, ey), strokeWidth = thickness)
}

// -------------------------------------------------------------------- 7
private fun DrawScope.drawDialHourIndices(center: Offset, rDial: Float) {
    // Stick-baton applied indices (12 of them); some are obscured by sub-dials in
    // a real Navitimer; we draw only 12, 1, 2, 4, 5, 7, 8, 10, 11 (skipping 3, 6, 9 = sub-dial cores)
    val skip = setOf(3, 6, 9)
    for (h in 0 until 12) {
        if (h in skip) continue
        val angle = h * 30.0 - 90.0
        val rad = angle * PI / 180.0
        val rIn = rDial * 0.62f
        val rOut = rDial * 0.74f
        val sx = center.x + (rIn * cos(rad)).toFloat()
        val sy = center.y + (rIn * sin(rad)).toFloat()
        val ex = center.x + (rOut * cos(rad)).toFloat()
        val ey = center.y + (rOut * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Hand,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = rDial * 0.024f
        )
    }
}

// -------------------------------------------------------------------- 8
private fun DrawScope.drawHands(center: Offset, rDial: Float, now: LocalDateTime) {
    val s = now.second + now.nanosecond / 1e9
    val mFull = now.minute + s / 60.0
    val hFull = (now.hour % 12) + mFull / 60.0
    val hourAngle = hFull * 30.0
    val minAngle = mFull * 6.0

    // Hour hand (broad arrow)
    drawHandShape(
        center = center,
        angleDeg = hourAngle.toFloat(),
        length = rDial * 0.42f,
        baseWidth = rDial * 0.045f,
        color = DialPalette.Hand
    )
    // Minute hand (longer)
    drawHandShape(
        center = center,
        angleDeg = minAngle.toFloat(),
        length = rDial * 0.62f,
        baseWidth = rDial * 0.035f,
        color = DialPalette.Hand
    )
    // Central red chronograph second hand — frozen at 12 o'clock (chrono idle)
    drawLine(
        color = DialPalette.SecondHand,
        start = center,
        end = Offset(center.x, center.y - rDial * 0.66f),
        strokeWidth = rDial * 0.012f
    )
    // Counterweight
    drawLine(
        color = DialPalette.SecondHand,
        start = center,
        end = Offset(center.x, center.y + rDial * 0.10f),
        strokeWidth = rDial * 0.018f
    )
    // Hub
    drawCircle(color = DialPalette.Hand, radius = rDial * 0.025f, center = center)
    drawCircle(color = DialPalette.SecondHand, radius = rDial * 0.012f, center = center)
}

private fun DrawScope.drawHandShape(
    center: Offset,
    angleDeg: Float,
    length: Float,
    baseWidth: Float,
    color: Color
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
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(baseLeftX, baseLeftY)
        lineTo(tipX, tipY)
        lineTo(baseRightX, baseRightY)
        close()
    }
    drawPath(path = path, color = color)
    // Outline
    drawPath(path = path, color = Color(0xFF1A1A1A), style = Stroke(width = length * 0.012f))
}

// -------------------------------------------------------------------- 9
private fun DrawScope.drawCrownAndPushers(center: Offset, rOuter: Float) {
    // Crown (right edge, slightly above center)
    val crownX = center.x + rOuter * 1.04f
    val crownY = center.y - rOuter * 0.04f
    drawRect(
        color = DialPalette.CrownSteel,
        topLeft = Offset(crownX - rOuter * 0.06f, crownY - rOuter * 0.10f),
        size = Size(rOuter * 0.08f, rOuter * 0.20f)
    )
    drawCircle(
        color = DialPalette.CrownSteel,
        radius = rOuter * 0.05f,
        center = Offset(crownX + rOuter * 0.02f, crownY)
    )
    // Pushers
    drawRect(
        color = DialPalette.CrownSteel,
        topLeft = Offset(crownX - rOuter * 0.05f, crownY - rOuter * 0.30f),
        size = Size(rOuter * 0.06f, rOuter * 0.10f)
    )
    drawRect(
        color = DialPalette.CrownSteel,
        topLeft = Offset(crownX - rOuter * 0.05f, crownY + rOuter * 0.20f),
        size = Size(rOuter * 0.06f, rOuter * 0.10f)
    )
}

// -------------------------------------------------------------------- helpers

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
        text = androidx.compose.ui.text.AnnotatedString(text),
        style = TextStyle(
            color = color,
            fontSize = sizeSp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    )
    // Rotate so text is tangential (radial-up)
    val rot = angleDegFromTop + 90f
    rotate(rot, pivot = Offset(x, y)) {
        drawText(
            textLayoutResult = l,
            topLeft = Offset(x - l.size.width / 2f, y - l.size.height / 2f)
        )
    }
}

private fun DrawScope.drawTriangleAtAngle(
    center: Offset,
    angleDeg: Float,
    radius: Float,
    size: Float,
    color: Color,
    inward: Boolean
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
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(tipX, tipY)
        lineTo(baseCx + px, baseCy + py)
        lineTo(baseCx - px, baseCy - py)
        close()
    }
    drawPath(path, color = color)
}
