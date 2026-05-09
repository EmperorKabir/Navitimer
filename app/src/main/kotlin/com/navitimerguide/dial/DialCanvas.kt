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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.navitimerguide.R
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
    val wingsBitmap = ImageBitmap.imageResource(R.drawable.breitling_wings)
    Canvas(modifier = modifier) {
        val g = geom()
        drawCoinEdgeBaseplate(g)
        drawBezelInsertRecess(g)
        drawFixedChapterRing(g, measurer)
        drawDialBackground(g)
        drawSunburstOverlay(g)
        drawDialHighlight(g)
        drawBrandMarks(g, measurer, wingsBitmap)
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

/*
 * ----------------------------------------------------------------------
 *  Watch-face proportion reference (all values relative to rOuter, the
 *  outermost chrome bezel edge). Derived by measuring the Breitling
 *  Navitimer B01 Chronograph 46 photos in research/.
 * ----------------------------------------------------------------------
 *
 *   Concentric rings (radius from centre):
 *     0.00 .. 0.71   green sunburst dial
 *     0.71 .. 0.84   inner FIXED slide-rule scale (the rehaut)
 *     0.84 .. 0.86   thin step / shadow between rings
 *     0.86 .. 0.99   rotating bezel insert (slide-rule numerals)
 *     0.99 .. 1.00   chrome coin-edge teeth
 *
 *   Hour indices (within the green dial):
 *     inner end  ≈ 0.43 r  (just outside brand wordmark + sub-dial cores)
 *     outer end  ≈ 0.69 r  (right at the inner edge of the chapter ring)
 *     width      ≈ 0.018 r
 *     skipped at 3 / 6 / 9 (sub-dial cores)
 *
 *   Sub-dials (inside the dial):
 *     centre offset from dial centre  ≈ 0.30 r
 *     sub-dial radius                ≈ 0.155 r
 *
 *   Date window (inside the 6 o'clock sub-dial):
 *     width   ≈ 0.030 r   (taller than wide — portrait aperture, ~0.6:1)
 *     height  ≈ 0.050 r
 *     centre below sub-dial centre by 0.07 r
 *     bg = black; text = white
 *
 *   Brand stack (centred on dial X axis, Y from dial centre, NEGATIVE = up):
 *     Wings logo centre   ≈ -0.21 r   (visual scale 0.085 r tall, 0.18 r wide)
 *     BREITLING centre    ≈ -0.090 r   (font ≈ 0.065 r tall, bold + spaced)
 *     1884 centre         ≈ -0.030 r   (font ≈ 0.028 r tall, medium, very spaced)
 *     NAVITIMER centre    ≈ +0.010 r   (font ≈ 0.040 r tall, semi-bold)
 *     SWISS MADE centre   ≈ +0.59 r   (font ≈ 0.024 r tall, just above date sub-dial)
 *
 *   Crown (3 o'clock):
 *     body rectangle: 0.09 r wide × 0.20 r tall, anchored at rOuter on right
 *     cap rectangle:  0.08 r wide × 0.18 r tall, sits OUTSIDE the body
 *     reeded grip stripes on body + cap face
 *
 *   Pushers (top: 2 o'clock = 60° from N; bottom: 4 o'clock = 120° from N):
 *     pusher axis is RADIAL (perpendicular to the case rim at that angle)
 *     shaft length ≈ 0.06 r, cap depth ≈ 0.05 r, cap face ≈ 0.05 r wide × 0.11 r tall
 *     reeded grip stripes parallel to the cap's long axis
 * ----------------------------------------------------------------------
 */
private fun DrawScope.geom(): DialGeom {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    // Shrink the watch a touch so the crown + angled pushers fit inside the
    // canvas (they protrude about 9% of r past the case at 2/3/4 o'clock).
    val rOuter = (minOf(w, h) / 2f) * 0.88f
    val rBezelOuter = rOuter * 0.99f
    // Step gap between rotating bezel and fixed chapter ring tightened from
    // 0.02 r to 0.005 r so the outer and inner ticks visually almost meet
    // across a hairline step (per photo image 16).
    val rBezelInner = rOuter * 0.850f
    val rChapterOuter = rOuter * 0.845f
    val rChapterInner = rOuter * 0.71f
    val rDial = rChapterInner
    return DialGeom(w, h, cx, cy, Offset(cx, cy), rOuter, rBezelOuter, rBezelInner, rChapterOuter, rChapterInner, rDial)
}

// =============================================================== labels

/** Outer rotating bezel: every 5 in the upper half, every 1 in 10..25, every 5 in 30..55. */
private val OUTER_LABEL_SET: Set<Int> =
    (10..25).toSet() +
    setOf(30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95)

/** Inner fixed scale: like the outer, but in the upper half only every 10 (with 70/80/90 abbreviated as 7/8/9).
 *  Values that have a red triangle marker (10, 35, 40) are intentionally
 *  EXCLUDED so the marker triangle replaces the regular numeral. 36 keeps
 *  its red numeral (alongside the red triangle). */
private val INNER_LABEL_MAP: Map<Int, String> =
    (11..25).associateWith { it.toString() } +                       // 10 excluded
    listOf(30, 45, 50, 55).associateWith { it.toString() } +         // 35, 40 excluded
    mapOf(60 to "60", 70 to "7", 80 to "8", 90 to "9", 36 to "36")

/** Integer scale values where a RED TRIANGLE replaces the regular tick.
 *  Per photo image 21: the red arrows sit ON the markers, not next to them. */
private val OUTER_TICK_REPLACED_BY_TRIANGLE: Set<Int> = setOf(10, 36, 60)
private val INNER_TICK_REPLACED_BY_TRIANGLE: Set<Int> = setOf(10, 35, 36, 40, 60)
/** Outer values where the regular numeral is also hidden (pure triangle marker). */
private val OUTER_NUMERAL_HIDDEN: Set<Int> = setOf(10)

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

    // OUTER bezel layering (per photo image 16, real watch order outside-in):
    //   numerals at OUTER edge of bezel ring → ticks INWARD of numerals →
    //   step gap → inner ring.
    // Numerals occupy the outer ~30% of ring width; ticks occupy the inner ~70%.
    val numeralR = ringMid + ringWidth * 0.32f          // numeral centre, outer half
    val tickOuterR = ringMid + ringWidth * 0.10f         // tall-tick outer end, just inward of numerals
    val tallLen = (tickOuterR - g.rBezelInner)           // ticks reach down to bezel inner edge
    val medLen = tallLen * 0.62f
    val shortLen = tallLen * 0.42f

    for (v in allTickValues()) {
        val intV = round(v).toInt()
        val isInt = isInteger(v)
        // At values where a red triangle takes the tick's place, skip drawing
        // the normal white tick.
        if (isInt && intV in OUTER_TICK_REPLACED_BY_TRIANGLE) continue

        val isLabelled = isInt && intV in OUTER_LABEL_SET
        val rank = tickRank(v, isLabelled)
        val angle = DialMath.drawAngleDeg(v)
        val rad = angle * PI / 180.0

        val len = when (rank) {
            TickRank.TALL -> tallLen
            TickRank.MEDIUM -> medLen
            TickRank.SHORT -> shortLen
        }
        val sw = when (rank) {
            TickRank.TALL -> 1.8f
            TickRank.MEDIUM -> 1.2f
            TickRank.SHORT -> 0.95f
        }
        val tickInnerR = tickOuterR - len
        val sx = g.center.x + (tickInnerR * cos(rad)).toFloat()
        val sy = g.center.y + (tickInnerR * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuterR * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuterR * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = when (rank) {
                TickRank.TALL -> 0.95f
                TickRank.MEDIUM -> 0.85f
                TickRank.SHORT -> 0.75f
            }),
            start = Offset(sx, sy), end = Offset(ex, ey),
            strokeWidth = sw
        )
        if (isLabelled && intV !in OUTER_NUMERAL_HIDDEN) {
            val isRed = (intV == 60 || intV == 36)
            drawScaleNumeralUpright(
                measurer = measurer,
                text = intV.toString(),
                angleDegFromTop = angle.toFloat(),
                radius = numeralR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * (if (intV % 5 == 0) 0.058f else 0.040f) / density).sp
            )
        }
    }
    // Red triangles on outer at 10 / 36 / 60 — TIP at the step gap (just
    // past the bezel inner edge) so each triangle on the outer scale
    // visually TOUCHES the corresponding triangle on the inner scale at
    // the same value when the bezel is aligned (per image 21).
    listOf(10.0, 36.0, 60.0).forEach { v ->
        val angle = DialMath.drawAngleDeg(v)
        drawTriangleAtAngle(
            center = g.center, angleDeg = angle.toFloat(),
            radius = ringMid - ringWidth * 0.376f,    // base inside bezel ring's lower half
            size = ringWidth * 0.18f,
            color = DialPalette.Red, inward = true     // tip ≈ 0.847 r
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

    // INNER chapter-ring layering (per the photo's outside-in order):
    //   step gap → ticks at OUTER half of ring → numerals at INNER half (closest
    //   to dial centre) → green dial.
    val numeralR = g.rChapterInner + width * 0.20f       // numeral centre, inner half
    val tickOuterR = g.rChapterOuter                     // ticks start at chapter outer edge (just under step)
    val tickInnerEndForLong = numeralR + width * 0.18f   // long ticks stop just before numerals
    val tallLen = (tickOuterR - tickInnerEndForLong)
    val medLen = tallLen * 0.62f
    val shortLen = tallLen * 0.42f

    for (v in allTickValues()) {
        val intV = round(v).toInt()
        val isInt = isInteger(v)
        if (isInt && intV in INNER_TICK_REPLACED_BY_TRIANGLE) continue
        val isLabelled = isInt && intV in INNER_LABEL_MAP
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
            TickRank.MEDIUM -> 1.1f
            TickRank.SHORT -> 0.85f
        }
        val tickInnerR = tickOuterR - len
        val sx = g.center.x + (tickInnerR * cos(rad)).toFloat()
        val sy = g.center.y + (tickInnerR * sin(rad)).toFloat()
        val ex = g.center.x + (tickOuterR * cos(rad)).toFloat()
        val ey = g.center.y + (tickOuterR * sin(rad)).toFloat()
        drawLine(
            color = DialPalette.Numeral.copy(alpha = when (rank) {
                TickRank.TALL -> 0.9f
                TickRank.MEDIUM -> 0.8f
                TickRank.SHORT -> 0.7f
            }),
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
                radius = numeralR,
                center = g.center,
                color = if (isRed) DialPalette.Red else DialPalette.Numeral,
                sizeSp = (g.rOuter * (if (intV % 5 == 0) 0.048f else 0.034f) / density).sp
            )
        }
    }

    // Inner red-triangle markers — TIPS land at the step gap (~0.847 r) so
    // they touch the matching outer triangles when aligned (per image 21).
    // The triangle base is the smaller-radius end; tip points outward
    // toward the bezel.
    val triangleBaseR = midR + width * 0.341f
    val triangleSize = width * 0.18f
    val triangleSizeSmall = width * 0.14f

    // KM (text only) / STAT (triangle + "STAT. 40") / NAUT (triangle + "NAUT. 35")
    val markerLabelR = numeralR + width * 0.22f
    Markers.all.filter { it.side == ScaleSide.INNER && it.style != MarkerStyle.RED_NUMERAL }
        .forEach { m ->
            val angle = DialMath.drawAngleDeg(m.scaleValue)
            if (m.style == MarkerStyle.TRIANGLE_OUTWARD) {
                drawTriangleAtAngle(
                    center = g.center, angleDeg = angle.toFloat(),
                    radius = triangleBaseR, size = triangleSize,
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

    // Single red triangle at inner 10 — the unit index. No bracketing pair,
    // no numeral — just one triangle, matching image 20's "two triangles
    // total" instruction (one outer + one inner).
    val red10Angle = DialMath.drawAngleDeg(10.0)
    drawTriangleAtAngle(
        center = g.center, angleDeg = red10Angle.toFloat(),
        radius = triangleBaseR, size = triangleSize,
        color = DialPalette.Red, inward = false
    )

    // Red triangle at inner 36 — the time-conversion marker.
    val red36Angle = DialMath.drawAngleDeg(36.0)
    drawTriangleAtAngle(
        center = g.center, angleDeg = red36Angle.toFloat(),
        radius = triangleBaseR, size = triangleSizeSmall,
        color = DialPalette.Red, inward = false
    )

    // (Inner red 60 / MPH triangle is drawn by the Markers.all loop above —
    //  it has style TRIANGLE_OUTWARD, so we don't need an explicit triangle here.)
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

private fun DrawScope.drawBrandMarks(g: DialGeom, measurer: TextMeasurer, wingsBitmap: ImageBitmap) {
    // Brand stack lives in the UPPER THIRD of the dial — well above the
    // hub — matching the photo. Order top-to-bottom: wings, BREITLING,
    // 1884, NAVITIMER. Stack ends at about y = -0.18 r.
    //
    // Wings are now rendered from a bitmap asset extracted from the
    // user's photo (res/drawable-nodpi/breitling_wings.png) for accurate
    // pixel-level fidelity that hand-drawn paths can't match.
    val targetWidth = g.rDial * 0.33f
    val aspect = wingsBitmap.width.toFloat() / wingsBitmap.height.toFloat()
    val targetHeight = targetWidth / aspect
    val logoCenterY = g.center.y - g.rDial * 0.42f
    val logoTopLeft = IntOffset(
        x = (g.center.x - targetWidth / 2f).toInt(),
        y = (logoCenterY - targetHeight / 2f).toInt()
    )
    drawImage(
        image = wingsBitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(wingsBitmap.width, wingsBitmap.height),
        dstOffset = logoTopLeft,
        dstSize = IntSize(targetWidth.toInt(), targetHeight.toInt())
    )

    drawCenteredText(measurer, "BREITLING",
        TextStyle(color = DialPalette.Numeral, fontSize = (g.rDial * 0.092f / density).sp,
            fontWeight = FontWeight.Bold, letterSpacing = (g.rDial * 0.010f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.32f))

    drawCenteredText(measurer, "1884",
        TextStyle(color = DialPalette.Numeral.copy(alpha = 0.9f),
            fontSize = (g.rDial * 0.040f / density).sp, fontWeight = FontWeight.Medium,
            letterSpacing = (g.rDial * 0.020f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.235f))

    drawCenteredText(measurer, "NAVITIMER",
        TextStyle(color = DialPalette.Numeral, fontSize = (g.rDial * 0.060f / density).sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (g.rDial * 0.012f / density).sp),
        Offset(g.center.x, g.center.y - g.rDial * 0.165f))

    // SWISS  MADE: curved along the bottom of the dial (just inside the
    // chapter ring), with the gap between the two words straddling the
    // 6 o'clock baton. Uses native canvas drawTextOnPath because Compose
    // doesn't expose a path-based text API.
    drawCurvedSwissMade(g)
}

private fun DrawScope.drawCurvedSwissMade(g: DialGeom) {
    drawIntoCanvas { canvas ->
        val arcRadius = g.rDial * 0.86f
        val rect = android.graphics.RectF(
            g.center.x - arcRadius, g.center.y - arcRadius,
            g.center.x + arcRadius, g.center.y + arcRadius
        )
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(230, 255, 255, 255)
            textSize = g.rDial * 0.055f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.10f
            isFakeBoldText = false
        }
        // Angles in Android: 0° = +x (right), 90° = +y (down), increasing CW.
        // For text reading LEFT-to-RIGHT along the bottom we sweep
        // counterclockwise (negative sweep) so the path goes from the
        // higher angle to the lower angle.
        //
        // SWISS — IMMEDIATELY left of the 6 o'clock baton (between 6 and 7,
        // not all the way out to 7). Per image 17, both words sit close to
        // the central baton.  Arc from 113° down to 95°.
        val swissPath = android.graphics.Path().apply {
            addArc(rect, 113f, -18f)
        }
        canvas.nativeCanvas.drawTextOnPath("SWISS", swissPath, 0f, 0f, paint)
        // MADE — IMMEDIATELY right of the baton (between 5 and 6). Arc
        // from 85° down to 67°.
        val madePath = android.graphics.Path().apply {
            addArc(rect, 85f, -18f)
        }
        canvas.nativeCanvas.drawTextOnPath("MADE", madePath, 0f, 0f, paint)
    }
}

private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer, text: String, style: TextStyle, centerTopLeft: Offset
) {
    val l = measurer.measure(androidx.compose.ui.text.AnnotatedString(text), style)
    drawText(textLayoutResult = l,
        topLeft = Offset(centerTopLeft.x - l.size.width / 2f, centerTopLeft.y))
}

/**
 * The Breitling Wings logo as printed on this Navitimer dial.
 * Reference: research/user-logo-3x.jpg (the user's photo, upscaled 3×)
 * and research/official-logo-3x.jpg (Breitling's stock soldier shot).
 *
 * Composition (left and right are mirror images about c.x):
 *   • OUTLINED wing — a closed contour with a curved upper "leading
 *     edge" that humps near the inner third, then sweeps down to a
 *     pointed tip. The bottom edge curves back gently.
 *   • DOTTED feather texture inside each wing — two rows of small
 *     bright circles (beadwork), 6 along the upper feather row and
 *     5 along the lower. Dot radius shrinks toward the tip.
 *   • CENTRAL anchor — a small inverted T beneath the wings' meeting
 *     point: short vertical stem + short horizontal crossbar.
 *   • WAVE underline — a 7-hump sinusoidal scroll spanning the full
 *     width, drawn just below the wings (the historic "wake" symbol).
 *
 * Coordinate system: the logo is centred at [c]; all dimensions scale
 * with [scale]. Total horizontal span ≈ 3.0 × scale; vertical ≈ 0.85.
 */
private fun DrawScope.drawBreitlingWings(c: Offset, scale: Float) {
    val color = DialPalette.Numeral
    val strokeW = scale * 0.06f

    // ---- Wing outlines ----
    for (side in listOf(-1f, 1f)) {
        val wingPath = Path().apply {
            moveTo(c.x + side * scale * 0.06f, c.y - scale * 0.06f)
            // Leading edge: short rise from centre, then long arched hump
            cubicTo(
                c.x + side * scale * 0.30f, c.y - scale * 0.42f,
                c.x + side * scale * 0.95f, c.y - scale * 0.42f,
                c.x + side * scale * 1.45f, c.y - scale * 0.10f
            )
            // Tip — small curl
            cubicTo(
                c.x + side * scale * 1.55f, c.y - scale * 0.02f,
                c.x + side * scale * 1.50f, c.y + scale * 0.04f,
                c.x + side * scale * 1.40f, c.y + scale * 0.04f
            )
            // Trailing (lower) edge sweeping back to centre
            cubicTo(
                c.x + side * scale * 1.00f, c.y + scale * 0.18f,
                c.x + side * scale * 0.45f, c.y + scale * 0.18f,
                c.x + side * scale * 0.06f, c.y + scale * 0.05f
            )
            close()
        }
        drawPath(wingPath, color = color, style = Stroke(width = strokeW))

        // ---- Dotted beadwork — top row (along the leading edge) ----
        run {
            val n = 6
            for (i in 0 until n) {
                val t = (i + 0.5) / n
                // Sample roughly along the upper arc
                val px = c.x + side * scale * (0.25f + t.toFloat() * 1.10f)
                val py = c.y - scale * (0.30f - (t.toFloat() - 0.5f) * (t.toFloat() - 0.5f) * 0.8f - 0.05f)
                val r = scale * (0.046f - t.toFloat() * 0.014f)
                drawCircle(color = color, radius = r, center = Offset(px, py))
            }
        }
        // ---- Dotted beadwork — lower row (just below) ----
        run {
            val n = 5
            for (i in 0 until n) {
                val t = (i + 0.5) / n
                val px = c.x + side * scale * (0.30f + t.toFloat() * 0.95f)
                val py = c.y - scale * 0.05f
                val r = scale * (0.034f - t.toFloat() * 0.010f)
                drawCircle(color = color, radius = r, center = Offset(px, py))
            }
        }
    }

    // ---- Central anchor stub (inverted T below the wings' join) ----
    drawLine(color = color,
        start = Offset(c.x, c.y - scale * 0.04f),
        end = Offset(c.x, c.y + scale * 0.18f),
        strokeWidth = strokeW * 0.85f)
    drawLine(color = color,
        start = Offset(c.x - scale * 0.10f, c.y + scale * 0.16f),
        end = Offset(c.x + scale * 0.10f, c.y + scale * 0.16f),
        strokeWidth = strokeW * 0.75f)

    // ---- Wave underline (7 humps) ----
    val waveY = c.y + scale * 0.32f
    val waveX0 = c.x - scale * 1.45f
    val waveX1 = c.x + scale * 1.45f
    val humps = 7
    val wavePath = Path().apply {
        moveTo(waveX0, waveY)
        for (i in 0 until humps) {
            val x0 = waveX0 + (waveX1 - waveX0) * i / humps
            val x1 = waveX0 + (waveX1 - waveX0) * (i + 1) / humps
            val xMid = (x0 + x1) / 2f
            val yPeak = waveY - scale * 0.08f
            quadraticBezierTo(xMid, yPeak, x1, waveY)
        }
    }
    drawPath(wavePath, color = color, style = Stroke(width = strokeW * 0.7f))
}

// =============================================================== sub-dials

private fun DrawScope.drawSubDialFaces(g: DialGeom, measurer: TextMeasurer) {
    val subR = g.rDial * 0.26f
    val offset = g.rDial * 0.45f
    // 9 o'clock — small running seconds (60-second face); labels at 60/20/40
    drawSubDialFace(
        center = Offset(g.center.x - offset, g.center.y), radius = subR,
        ticks = 60, majorEvery = 5, measurer = measurer,
        ringNumbers = listOf(60 to "60", 20 to "20", 40 to "40")
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
    // Date window inside the 12-hr sub-dial: a PORTRAIT aperture (taller
    // than wide) showing white numerals on the black date wheel, framed
    // by a thin chrome rim. Matches the slim vertical slot in the photo.
    val now = currentLocalDateTime()
    val dateBoxW = subR * 0.42f
    val dateBoxH = subR * 0.50f
    val dateTopLeft = Offset(hrCenter.x - dateBoxW / 2f, hrCenter.y + subR * 0.30f)
    drawRect(color = DialPalette.HandFrame,
        topLeft = Offset(dateTopLeft.x - 1.4f, dateTopLeft.y - 1.4f),
        size = Size(dateBoxW + 2.8f, dateBoxH + 2.8f))
    drawRect(color = Color(0xFF0A0A0A), topLeft = dateTopLeft, size = Size(dateBoxW, dateBoxH))
    val l = measurer.measure(
        androidx.compose.ui.text.AnnotatedString(now.dayOfMonth.toString()),
        TextStyle(color = Color.White, fontSize = (subR * 0.42f / density).sp,
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
            TextStyle(color = DialPalette.Numeral, fontSize = (radius * 0.30f / density).sp,
                fontWeight = FontWeight.SemiBold)
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
    val subR = g.rDial * 0.26f
    val offset = g.rDial * 0.45f
    val secondsCenter = Offset(g.center.x - offset, g.center.y)
    val s = now.second + now.nanosecond / 1e9
    val angle = (s * 6.0).toFloat()
    drawSubDialHand(secondsCenter, subR * 0.85f, angle, DialPalette.Hand, subR * 0.04f)
}

private fun DrawScope.drawChronoMinAndHourHands(g: DialGeom, chronoMs: Long) {
    val subR = g.rDial * 0.26f
    val offset = g.rDial * 0.45f
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
    // Sub-dial outer edge is at offset 0.45 + radius 0.26 = 0.71 r from
    // dial centre. Hour markers at 3, 6, 9 are TRUNCATED so they sit
    // entirely outside their adjacent sub-dial.
    val rOut = g.rDial * 0.95f             // outer end (inner edge of chapter ring)
    val rInFull = g.rDial * 0.62f          // inner end for "long" markers (1, 2, 4, 5, 7, 8, 10, 11)
    val rInShort = g.rDial * 0.74f         // inner end for "short" markers at 3, 6, 9
    val width = g.rDial * 0.022f

    fun drawMarker(angle: Double, rIn: Float, rOut: Float, w: Float, xOffset: Float = 0f) {
        val rad = angle * PI / 180.0
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val perpX = (-sinA) * w
        val perpY = cosA * w
        val tipX = g.center.x + (rOut * cos(rad)).toFloat() + xOffset * (-sinA)
        val tipY = g.center.y + (rOut * sin(rad)).toFloat() + xOffset * cosA
        val baseX = g.center.x + (rIn * cos(rad)).toFloat() + xOffset * (-sinA)
        val baseY = g.center.y + (rIn * sin(rad)).toFloat() + xOffset * cosA
        val path = Path().apply {
            moveTo(tipX + perpX, tipY + perpY)
            lineTo(tipX - perpX, tipY - perpY)
            lineTo(baseX - perpX, baseY - perpY)
            lineTo(baseX + perpX, baseY + perpY)
            close()
        }
        drawPath(path = path, color = DialPalette.Hand)
        // Centre highlight stripe (chamfered 3D look)
        val centreW = w * 0.20f
        val centrePath = Path().apply {
            val cpX = (-sinA) * centreW
            val cpY = cosA * centreW
            moveTo(tipX + cpX, tipY + cpY)
            lineTo(tipX - cpX, tipY - cpY)
            lineTo(baseX - cpX, baseY - cpY)
            lineTo(baseX + cpX, baseY + cpY)
            close()
        }
        drawPath(path = centrePath, color = DialPalette.HandFrame)
        drawPath(path = path, color = Color(0xFF1A1A1A), style = Stroke(width = 0.9f))
    }

    for (h in 0 until 12) {
        val angle = h * 30.0 - 90.0
        when (h) {
            0 -> {
                // 12 o'clock — DOUBLE baton marker, slightly shorter than the
                // single ones, with a small horizontal gap between the two.
                val gap = g.rDial * 0.030f
                val shortIn = g.rDial * 0.66f
                val shortOut = g.rDial * 0.92f
                drawMarker(angle, shortIn, shortOut, width * 0.85f, xOffset = -gap)
                drawMarker(angle, shortIn, shortOut, width * 0.85f, xOffset = +gap)
            }
            3, 9 -> drawMarker(angle, rInShort, rOut, width)   // short, sub-dial-truncated
            6 -> drawMarker(angle, rInShort, rOut, width)      // short — SWISS / MADE flank
            else -> drawMarker(angle, rInFull, rOut, width)
        }
    }
}

// =============================================================== hands (time + chrono)

private fun DrawScope.drawTimeHands(g: DialGeom, now: LocalDateTime) {
    val s = now.second + now.nanosecond / 1e9
    val mFull = now.minute + s / 60.0
    val hFull = (now.hour % 12) + mFull / 60.0
    val hourAngle = (hFull * 30.0).toFloat()
    val minAngle = (mFull * 6.0).toFloat()
    // Hand lengths per photo: hour reaches the sub-dial markers (~0.55 r),
    // minute reaches almost to the chapter ring inner edge (~0.85 r).
    drawBatonHand(g.center, hourAngle, length = g.rDial * 0.58f,
        outerW = g.rDial * 0.050f, innerW = g.rDial * 0.030f)
    drawBatonHand(g.center, minAngle, length = g.rDial * 0.92f,
        outerW = g.rDial * 0.038f, innerW = g.rDial * 0.022f)
}

/**
 * Central red chronograph seconds hand. Reference: user's image #6 crop.
 * Composition (from tip to tail):
 *   • Long red NEEDLE from the hub to ~0.66 r tip.
 *   • Short CHROME counterweight stem extending the OPPOSITE direction
 *     from the hub by ~0.14 r (matches the photo — the stem below the
 *     hub is silver, not red).
 *   • Small RED disc at the very end of the chrome stem.
 */
private fun DrawScope.drawChronoSecondsHand(g: DialGeom, chronoMs: Long) {
    val secs = (chronoMs / 1000.0) % 60.0
    val angleDeg = (secs * 6.0)
    val rad = (angleDeg - 90.0) * PI / 180.0
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    // Red needle (hub → tip)
    val tipLen = g.rDial * 0.66f
    val tipX = g.center.x + tipLen * cosA
    val tipY = g.center.y + tipLen * sinA
    drawLine(
        color = DialPalette.SecondHand,
        start = g.center,
        end = Offset(tipX, tipY),
        strokeWidth = g.rDial * 0.013f
    )

    // Chrome counterweight stem (hub → tail, OPPOSITE direction).
    // Per image 19: the stem is silver and TAPERS slightly outward toward
    // the tail (no red dot), like a small chrome flare at the end.
    val tailLen = g.rDial * 0.16f
    val tailX = g.center.x - tailLen * cosA
    val tailY = g.center.y - tailLen * sinA
    val perpX = -sinA
    val perpY = cosA
    val widthHubOuter = g.rDial * 0.012f
    val widthTailOuter = g.rDial * 0.020f
    val widthHubInner = g.rDial * 0.008f
    val widthTailInner = g.rDial * 0.014f
    val stemFrame = Path().apply {
        moveTo(g.center.x + perpX * widthHubOuter, g.center.y + perpY * widthHubOuter)
        lineTo(g.center.x - perpX * widthHubOuter, g.center.y - perpY * widthHubOuter)
        lineTo(tailX - perpX * widthTailOuter, tailY - perpY * widthTailOuter)
        lineTo(tailX + perpX * widthTailOuter, tailY + perpY * widthTailOuter)
        close()
    }
    drawPath(stemFrame, color = DialPalette.HandFrame)
    val stemInner = Path().apply {
        moveTo(g.center.x + perpX * widthHubInner, g.center.y + perpY * widthHubInner)
        lineTo(g.center.x - perpX * widthHubInner, g.center.y - perpY * widthHubInner)
        lineTo(tailX - perpX * widthTailInner, tailY - perpY * widthTailInner)
        lineTo(tailX + perpX * widthTailInner, tailY + perpY * widthTailInner)
        close()
    }
    drawPath(stemInner, color = DialPalette.Hand)
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
    drawAngledChronoControl(g, angleFromNorthDeg = 60.0,                  // 2 o'clock — top pusher
        shaftLen = g.rOuter * 0.020f, shaftHalfW = g.rOuter * 0.030f,
        capDepth = g.rOuter * 0.060f, capHalfW = g.rOuter * 0.065f,
        reeded = true)
    drawAngledChronoControl(g, angleFromNorthDeg = 120.0,                 // 4 o'clock — bottom pusher
        shaftLen = g.rOuter * 0.020f, shaftHalfW = g.rOuter * 0.030f,
        capDepth = g.rOuter * 0.060f, capHalfW = g.rOuter * 0.065f,
        reeded = true)
    // Crown stem nearly zero — cap sits almost flush with the case rim,
    // matching photo image 15.
    drawAngledChronoControl(g, angleFromNorthDeg = 90.0,                  // 3 o'clock — crown
        shaftLen = g.rOuter * 0.015f, shaftHalfW = g.rOuter * 0.045f,
        capDepth = g.rOuter * 0.080f, capHalfW = g.rOuter * 0.090f,
        reeded = true)
}

/**
 * One chronograph control (crown or pusher) drawn at an arbitrary clock
 * position. Its axis is RADIAL — perpendicular to the case rim at that
 * angle — so top/bottom pushers tilt up-right and down-right just like
 * the real watch.
 *
 *  - The shaft is a parallelogram from the case rim outward along the
 *    radial direction.
 *  - The cap is a rectangle perpendicular to the radial direction —
 *    appears as a vertical-ish rectangle from face-on. The cap is wider
 *    than its depth (matching the photo's mushroom-cap pushers).
 *  - Reeded grip lines run along the cap's long axis (perpendicular to
 *    the radial), giving the brushed-steel look.
 */
private fun DrawScope.drawAngledChronoControl(
    g: DialGeom,
    angleFromNorthDeg: Double,
    shaftLen: Float,
    shaftHalfW: Float,
    capDepth: Float,
    capHalfW: Float,
    reeded: Boolean
) {
    // Convert "degrees clockwise from 12 o'clock" → screen angle (0 = +x).
    val screenAngleDeg = angleFromNorthDeg - 90.0
    val rad = screenAngleDeg * PI / 180.0
    val nx = cos(rad).toFloat()   // radial-outward x
    val ny = sin(rad).toFloat()   // radial-outward y
    val px = -ny                  // perpendicular-to-radial x
    val py = nx                   // perpendicular-to-radial y

    // Anchor on the case rim (slightly inside rOuter so the shaft visibly
    // emerges from the bezel, not the air beyond).
    val rRim = g.rOuter * 0.985f
    val ax = g.center.x + rRim * nx
    val ay = g.center.y + rRim * ny

    // Far end of the shaft (where the cap base sits).
    val sx = ax + nx * shaftLen
    val sy = ay + ny * shaftLen

    // Far face of the cap.
    val fx = sx + nx * capDepth
    val fy = sy + ny * capDepth

    // Shaft as a parallelogram.
    val shaftPath = Path().apply {
        moveTo(ax + px * shaftHalfW, ay + py * shaftHalfW)
        lineTo(ax - px * shaftHalfW, ay - py * shaftHalfW)
        lineTo(sx - px * shaftHalfW, sy - py * shaftHalfW)
        lineTo(sx + px * shaftHalfW, sy + py * shaftHalfW)
        close()
    }
    drawPath(shaftPath, color = DialPalette.SteelMid)
    drawPath(shaftPath, color = DialPalette.SteelGroove, style = Stroke(width = 0.8f))

    // Cap as a rectangle perpendicular to the axis (taller than wide on
    // the screen because capHalfW > capDepth/2 and the long side is along
    // the perpendicular axis).
    val capPath = Path().apply {
        moveTo(sx + px * capHalfW, sy + py * capHalfW)
        lineTo(sx - px * capHalfW, sy - py * capHalfW)
        lineTo(fx - px * capHalfW, fy - py * capHalfW)
        lineTo(fx + px * capHalfW, fy + py * capHalfW)
        close()
    }
    drawPath(capPath, color = DialPalette.SteelLight)
    drawPath(capPath, color = DialPalette.SteelMid, style = Stroke(width = 1.0f))

    // Reeded grip stripes on the cap face — lines parallel to the radial
    // axis, evenly spaced across the perpendicular (cap-long) direction.
    if (reeded) {
        val numLines = 7
        for (i in 1..numLines) {
            val frac = (i.toDouble() / (numLines + 1)) * 2.0 - 1.0   // -1..+1
            val offX = (px * capHalfW * frac).toFloat()
            val offY = (py * capHalfW * frac).toFloat()
            drawLine(
                color = DialPalette.SteelGroove,
                start = Offset(sx + offX + nx * capDepth * 0.10f,
                               sy + offY + ny * capDepth * 0.10f),
                end = Offset(fx + offX - nx * capDepth * 0.10f,
                             fy + offY - ny * capDepth * 0.10f),
                strokeWidth = 1.0f
            )
        }
    }
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
