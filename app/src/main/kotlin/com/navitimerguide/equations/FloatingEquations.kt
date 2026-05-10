package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.navitimerguide.dial.DialMath
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Plain-English equation panel.
 * - No hyphens in scale references ("inner 10", not "inner-10").
 * - Singular / plural words match the value ("1 hour", "2 hours").
 * - The bezel-derived multiplier and the user's typed Outer / Inner
 *   anchors drive every live result.
 */
@Composable
fun FloatingEquations(
    rotationDegrees: Double,
    outer: String,
    inner: String,
    modifier: Modifier = Modifier
) {
    val k = DialMath.multiplierFromRotation(rotationDegrees)
    val x = outer.toDoubleOrNull()
    val y = inner.toDoubleOrNull()
    val mph = DialMath.outerValueAtInner(60.0, rotationDegrees)
    val above60 = DialMath.outerValueAtInner(60.0, rotationDegrees)
    val above36 = DialMath.outerValueAtInner(36.0, rotationDegrees)
    // Slide-rule conversion ratios — what the bezel ACTUALLY reads when
    // you do the alignment, not the textbook unit-conversion factor.
    val statKmRatio = DialMath.KM_MARKER / DialMath.STAT_MARKER  // ≈ 1.605
    val nautKmRatio = DialMath.KM_MARKER / DialMath.NAUT_MARKER  // ≈ 1.848

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Live equations", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold)

        // ---------------- Division
        Section(
            title = "Division",
            explanation =
                "Pick a number on the outer ring and turn the bezel so it lines " +
                "up with a number on the inner ring. The bezel has just done a " +
                "division for you — to read the answer, look at the position " +
                "above inner 10.\n\n" +
                "Alternative: line up an inner number with an outer number; the " +
                "answer (inner divided by outer) is the inner value below outer 10.",
            live = if (x != null && y != null && y != 0.0)
                "${fmt(x)} on the outer ring divided by ${fmt(y)} on the inner ring gives ${fmt(x / y)}."
            else "Type a number for Outer and Inner above to see the answer."
        )

        // ---------------- Multiplication
        Section(
            title = "Multiplication",
            explanation =
                "The same idea works the other way. Line up outer 10 with any " +
                "number on the inner ring — that number becomes your multiplier. " +
                "Pick a value on the inner side and read the bigger result on the " +
                "outer side directly above it.\n\n" +
                "Alternative: line up inner 10 with an outer multiplier; any " +
                "inner value times that multiplier reads on the outer scale " +
                "above it. Either direction works — pick whichever is more " +
                "convenient for the numbers you've got.",
            live = if (y != null)
                "The bezel is set to multiply by ${fmt(k)}, so ${fmt(y)} becomes ${fmt(y * k)}."
            else "Slide the bezel to set a multiplier; the live value will appear here."
        )

        // ---------------- Speed
        Section(
            title = "Speed in miles per hour",
            explanation =
                "Speed is distance divided by time, scaled to per hour. Line up " +
                "your distance in miles on the outer ring with how long it took " +
                "in minutes on the inner ring. The mph reading appears at the " +
                "red 60 marker at 12 o'clock (the MPH index).",
            live = if (x != null && y != null && y != 0.0)
                "Travelling ${fmt(x)} ${unit(x, "mile", "miles")} in ${fmt(y)} " +
                "${unit(y, "minute", "minutes")} works out to ${fmt(x / y * 60.0)} mph."
            else "Speed at the MPH index right now reads ${fmt(mph)} mph."
        )

        // ---------------- Time (inverse of Speed)
        Section(
            title = "Time for a journey",
            explanation =
                "Once a speed is set on the dial (your mph above the 12 o'clock " +
                "MPH index), the bezel will tell you how long any distance will " +
                "take. Read off the inner ring directly under any outer-ring " +
                "distance — that's the time in minutes.",
            live = if (x != null && mph.isFinite() && mph > 0)
                "At ${fmt(mph)} mph, ${fmt(x)} ${unit(x, "mile", "miles")} takes " +
                "${fmt(x * 60.0 / mph)} ${unit(x * 60.0 / mph, "minute", "minutes")}."
            else "Set a speed on the dial to see how long a distance takes."
        )

        // ---------------- Statute miles ↔ km
        Section(
            title = "Miles to kilometres",
            explanation =
                "Line up your miles value on the outer ring with the small red " +
                "STAT triangle (between 35 and 40 on the inner ring). Now read " +
                "the kilometres above the KM marker (between 60 and 65). The " +
                "slide-rule does the multiplication by 1.609 in one move.",
            live = if (x != null) {
                val kmVal = x * statKmRatio
                "${fmt(x)} ${unit(x, "mile", "miles")} reads " +
                "${fmt(kmVal)} ${unit(kmVal, "kilometre", "kilometres")} at the KM marker " +
                "(slide-rule ratio ${fmt(statKmRatio)} vs the textbook 1.609)."
            } else "Slide-rule reads miles × ${fmt(statKmRatio)} at the KM marker " +
                "(textbook conversion is × 1.609)."
        )

        // ---------------- Nautical miles ↔ km
        Section(
            title = "Nautical miles to kilometres",
            explanation =
                "A nautical mile is 1.852 kilometres — used at sea and in the air. " +
                "Line up your nautical-mile value with the NAUT triangle (just " +
                "below the STAT triangle on the inner ring). Read the kilometres " +
                "above the KM marker, exactly as for statute miles.",
            live = if (x != null) {
                val kmVal = x * nautKmRatio
                "${fmt(x)} ${unit(x, "nautical mile", "nautical miles")} reads " +
                "${fmt(kmVal)} ${unit(kmVal, "kilometre", "kilometres")} at the KM marker " +
                "(slide-rule ratio ${fmt(nautKmRatio)} vs the textbook 1.852)."
            } else "Slide-rule reads nautical miles × ${fmt(nautKmRatio)} at the KM marker " +
                "(textbook conversion is × 1.852)."
        )

        // ---------------- Hours / Minutes / Seconds
        Section(
            title = "Hours, minutes and seconds",
            explanation =
                "There are 60 minutes in an hour and 60 seconds in a minute, so " +
                "60 × 60 = 3600 seconds in an hour. The dial has red markers " +
                "right at 60 and at 36 (which stands for 3600). Line up your " +
                "hours value on the outer ring against inner 10, then read the " +
                "minutes above inner 60 and the seconds above inner 36 — all in " +
                "one go.",
            live = "${fmt(k)} ${unit(k, "hour", "hours")} is the same as " +
                "${fmt(k * 60)} ${unit(k * 60, "minute", "minutes")}, or " +
                "${fmt(k * 3600)} ${unit(k * 3600, "second", "seconds")}.\n" +
                "On the dial: above inner 60 reads ${fmt(above60)}, " +
                "and above inner 36 reads ${fmt(above36)}."
        )
    }
}

@Composable
private fun Section(title: String, explanation: String, live: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.size(2.dp))
        Text(explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(4.dp))
        Text(live, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary)
    }
}

private fun fmt(value: Double): String {
    if (!value.isFinite()) return "—"
    val rounded = value.roundToInt()
    if (abs(value - rounded) < 1e-6 && abs(value) < 1e9) {
        return rounded.toString()
    }
    val s = "%.4f".format(value).trimEnd('0').trimEnd('.')
    return s.ifEmpty { "0" }
}

/** Returns [singular] when the value is exactly 1 (within tolerance), else [plural]. */
private fun unit(value: Double, singular: String, plural: String): String {
    val isOne = abs(value - 1.0) < 1e-6
    return if (isOne) singular else plural
}
