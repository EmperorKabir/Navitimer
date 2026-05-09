package com.navitimerguide.dial

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Hand-drawn replica of the Breitling Navitimer bezel numeral typeface.
 * The actual Breitling face is proprietary and not publicly available;
 * these shapes are reconstructed from photo evidence (images 27 / 29 /
 * 30 of the user's watch) — tall, narrow, condensed, slab-leaning,
 * with crisp butt-cap terminals and right-angle joins.
 *
 * Each glyph is laid out in a unit-box [0..GLYPH_W] × [0..GLYPH_H]
 * (origin top-left). The drawing helpers translate / scale / rotate
 * that unit box into screen space.
 */
private const val GLYPH_W = 0.62f
private const val GLYPH_H = 1.00f
// Stroke at 22% of height — heavy enough to read solidly, thin enough that
// adjacent strokes don't overlap inside narrow digits like "8" / "0".
private const val STROKE_FRAC = 0.22f
private const val DIGIT_GAP_FRAC = 0.10f

/**
 * Draw an uppercase numeric string [text] horizontally, then rotate so it
 * reads upright at angular position [angleDegFromTop] on a circular dial
 * (degrees clockwise from 3 o'clock; the same convention the rest of the
 * dial uses). The string is centred at radius [radius] from [center].
 */
fun DrawScope.drawNavitimerNumeralUpright(
    text: String,
    angleDegFromTop: Float,
    radius: Float,
    center: Offset,
    height: Float,
    color: Color,
    bold: Boolean = false
) {
    val rad = angleDegFromTop * Math.PI.toFloat() / 180f
    val x = center.x + radius * kotlin.math.cos(rad)
    val y = center.y + radius * kotlin.math.sin(rad)
    val rotation = angleDegFromTop + 90f
    rotate(rotation, pivot = Offset(x, y)) {
        drawNavitimerNumeralAt(text, Offset(x, y), height, color, bold)
    }
}

/**
 * Draw [text] horizontally with its mid-baseline at [center]. No rotation.
 * Each glyph in the string is rendered through [drawNavitimerDigit].
 */
fun DrawScope.drawNavitimerNumeralAt(
    text: String,
    center: Offset,
    height: Float,
    color: Color,
    bold: Boolean = false
) {
    val w = height * GLYPH_W
    val gap = height * DIGIT_GAP_FRAC
    val totalW = text.length * w + (text.length - 1).coerceAtLeast(0) * gap
    var cursorX = center.x - totalW / 2f
    val top = center.y - height / 2f
    for (ch in text) {
        if (ch.isDigit()) {
            drawNavitimerDigit(ch, Offset(cursorX, top), w, height, color, bold)
        }
        // Non-digit chars (e.g. '.') fall through unrendered for now.
        cursorX += w + gap
    }
}

/**
 * Draw a single digit [d] in the box of size [w]×[h] with origin at
 * [topLeft]. The shapes below are tuned to the Navitimer photos.
 */
private fun DrawScope.drawNavitimerDigit(
    d: Char,
    topLeft: Offset,
    w: Float,
    h: Float,
    color: Color,
    bold: Boolean
) {
    val sw = h * STROKE_FRAC * (if (bold) 1.10f else 1.0f)
    val left = topLeft.x
    val top = topLeft.y
    val right = left + w
    val bottom = top + h
    val midX = left + w / 2f
    @Suppress("UNUSED_VARIABLE") val midY = top + h / 2f
    // Inset by half-stroke so outer edges align to the box, not exceed it.
    val pad = sw / 2f
    val innerLeft = left + pad
    val innerRight = right - pad
    val innerTop = top + pad
    val innerBot = bottom - pad
    // Round caps + round joins keep the thick strokes looking smooth at
    // intersections (no spiky miter overshoots at 90° corners).
    val stroke = Stroke(
        width = sw,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val path = Path()

    when (d) {
        '0' -> {
            // Tall, narrow, slightly squared oval — like the Navitimer "0".
            // Use a rounded rectangle with corner radius = w*0.45 for a soft
            // rectangular-oval feel rather than a pure ellipse.
            val cornerR = w * 0.45f
            roundRectPath(path, innerLeft, innerTop, innerRight, innerBot, cornerR)
        }
        '1' -> {
            // Vertical bar with a short angled flag at the top-left.
            path.moveTo(innerLeft + w * 0.15f, innerTop + h * 0.18f)
            path.lineTo(midX, innerTop)
            path.lineTo(midX, innerBot)
        }
        '2' -> {
            // Top half-circle, diagonal down-left, bottom horizontal bar.
            path.moveTo(innerLeft, innerTop + h * 0.30f)
            path.cubicTo(
                innerLeft, innerTop + h * 0.05f,
                innerRight, innerTop + h * 0.05f,
                innerRight, innerTop + h * 0.32f
            )
            path.lineTo(innerLeft, innerBot)
            path.lineTo(innerRight, innerBot)
        }
        '3' -> {
            // Top arc (open-left), short middle bar, bottom arc (open-left).
            // Rendered as one continuous stroke from upper-left around.
            path.moveTo(innerLeft, innerTop + h * 0.05f)
            path.cubicTo(
                midX, innerTop - h * 0.05f,
                innerRight, innerTop + h * 0.05f,
                innerRight, innerTop + h * 0.30f
            )
            path.cubicTo(
                innerRight, innerTop + h * 0.45f,
                innerLeft + w * 0.50f, innerTop + h * 0.50f,
                innerLeft + w * 0.30f, innerTop + h * 0.50f
            )
            path.moveTo(innerLeft + w * 0.30f, innerTop + h * 0.50f)
            path.cubicTo(
                innerLeft + w * 0.50f, innerTop + h * 0.50f,
                innerRight, innerTop + h * 0.55f,
                innerRight, innerTop + h * 0.70f
            )
            path.cubicTo(
                innerRight, innerBot + h * 0.05f,
                midX, innerBot + h * 0.05f,
                innerLeft, innerBot - h * 0.05f
            )
        }
        '4' -> {
            // Diagonal from lower-left up to mid; horizontal across at mid;
            // vertical right-side bar from top to bottom.
            path.moveTo(innerLeft, innerTop + h * 0.62f)
            path.lineTo(innerRight - sw * 0.3f, innerTop + h * 0.62f)
            path.moveTo(innerRight - sw * 0.3f, innerTop)
            path.lineTo(innerRight - sw * 0.3f, innerBot)
            path.moveTo(innerRight - sw * 0.3f, innerTop)
            path.lineTo(innerLeft, innerTop + h * 0.62f)
        }
        '5' -> {
            // Top horizontal, vertical left, then a half-loop to the bottom.
            path.moveTo(innerRight, innerTop)
            path.lineTo(innerLeft, innerTop)
            path.lineTo(innerLeft, innerTop + h * 0.45f)
            path.lineTo(innerLeft + w * 0.55f, innerTop + h * 0.45f)
            path.cubicTo(
                innerRight, innerTop + h * 0.45f,
                innerRight, innerBot,
                innerLeft + w * 0.55f, innerBot
            )
            path.cubicTo(
                innerLeft + w * 0.10f, innerBot,
                innerLeft, innerBot - h * 0.05f,
                innerLeft, innerBot - h * 0.15f
            )
        }
        '6' -> {
            // Long curve from upper-right down to lower-left, then a
            // closed rounded-rectangle for the lower half.
            path.moveTo(innerRight, innerTop + h * 0.10f)
            path.cubicTo(
                innerRight - w * 0.20f, innerTop - h * 0.05f,
                innerLeft, innerTop + h * 0.10f,
                innerLeft, innerTop + h * 0.50f
            )
            // Lower oval — closed shape.
            path.moveTo(innerLeft, innerTop + h * 0.55f)
            path.cubicTo(
                innerLeft, innerTop + h * 0.40f,
                innerRight, innerTop + h * 0.40f,
                innerRight, innerTop + h * 0.55f
            )
            path.lineTo(innerRight, innerBot - h * 0.10f)
            path.cubicTo(
                innerRight, innerBot + h * 0.05f,
                innerLeft, innerBot + h * 0.05f,
                innerLeft, innerBot - h * 0.10f
            )
            path.lineTo(innerLeft, innerTop + h * 0.55f)
        }
        '7' -> {
            // Top horizontal, then a long diagonal to the lower-left.
            path.moveTo(innerLeft, innerTop)
            path.lineTo(innerRight, innerTop)
            path.lineTo(innerLeft + w * 0.20f, innerBot)
        }
        '8' -> {
            // Two stacked ovals — small inset on the upper one to match
            // the photo where the upper bowl is visibly narrower.
            val midSplit = innerTop + h * 0.50f
            // Upper oval (slightly inset)
            ovalSubpath(path, innerLeft + w * 0.06f, innerTop,
                innerRight - w * 0.06f, midSplit)
            // Lower oval (full width)
            ovalSubpath(path, innerLeft, midSplit, innerRight, innerBot)
        }
        '9' -> {
            // Closed top oval, then a straight stroke down the right side
            // continuing into a curve to lower-left (mirror of '6').
            ovalSubpath(path, innerLeft, innerTop, innerRight, innerTop + h * 0.50f)
            path.moveTo(innerRight, innerTop + h * 0.45f)
            path.cubicTo(
                innerRight, innerTop + h * 0.55f,
                innerRight - w * 0.10f, innerBot,
                innerLeft, innerBot - h * 0.10f
            )
        }
    }

    drawPath(path = path, color = color, style = stroke)
}

/** Append a stroked rounded-rectangle subpath to [path]. */
private fun roundRectPath(
    path: Path,
    left: Float, top: Float, right: Float, bottom: Float, r: Float
) {
    val rr = r.coerceAtMost((right - left) / 2f).coerceAtMost((bottom - top) / 2f)
    path.moveTo(left + rr, top)
    path.lineTo(right - rr, top)
    path.cubicTo(right, top, right, top, right, top + rr)
    path.lineTo(right, bottom - rr)
    path.cubicTo(right, bottom, right, bottom, right - rr, bottom)
    path.lineTo(left + rr, bottom)
    path.cubicTo(left, bottom, left, bottom, left, bottom - rr)
    path.lineTo(left, top + rr)
    path.cubicTo(left, top, left, top, left + rr, top)
    path.close()
}

/** Append a closed oval subpath approximated by 4 cubic Beziers. */
private fun ovalSubpath(
    path: Path,
    left: Float, top: Float, right: Float, bottom: Float
) {
    val cx = (left + right) / 2f
    val cy = (top + bottom) / 2f
    val rx = (right - left) / 2f
    val ry = (bottom - top) / 2f
    val k = 0.5522847f  // magic Bezier-circle constant
    path.moveTo(cx, top)
    path.cubicTo(cx + rx * k, top, right, cy - ry * k, right, cy)
    path.cubicTo(right, cy + ry * k, cx + rx * k, bottom, cx, bottom)
    path.cubicTo(cx - rx * k, bottom, left, cy + ry * k, left, cy)
    path.cubicTo(left, cy - ry * k, cx - rx * k, top, cx, top)
    path.close()
}
