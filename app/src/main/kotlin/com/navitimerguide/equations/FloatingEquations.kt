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
import kotlin.math.roundToInt

/**
 * Plain-English equation panel. Each section has:
 *   • Title (the operation name)
 *   • A short, friendly explanation that says what the bezel is doing
 *     in everyday language — no maths jargon and no "Now" word.
 *   • The live result in a short sentence using the user's typed
 *     Outer / Inner anchors and the bezel's current rotation.
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
                "division for you. To read the answer, look at the position " +
                "above inner-10.",
            live = if (x != null && y != null && y != 0.0)
                "${fmt(x)} on the outer divided by ${fmt(y)} on the inner gives ${fmt(x / y)}."
            else "Type a number for Outer and Inner above to see the answer."
        )

        // ---------------- Multiplication
        Section(
            title = "Multiplication",
            explanation =
                "The same idea works the other way around. Line up the outer-10 " +
                "with any number on the inner ring; that number is your " +
                "multiplier. Whatever you read on the inner ring will appear " +
                "multiplied on the outer ring above it.",
            live = if (y != null)
                "The bezel is set to multiply by ${fmt(k)}, so ${fmt(y)} becomes ${fmt(y * k)}."
            else "Set a multiplier by sliding the bezel; the live value will appear here."
        )

        // ---------------- Speed / Time / Distance
        Section(
            title = "Speed (miles per hour)",
            explanation =
                "Speed is distance divided by time, scaled to per-hour. Line up " +
                "your distance in miles on the outer ring with the time you took " +
                "in minutes on the inner ring. The mph reading is at the red 60 " +
                "marker at 12 o'clock (the MPH index).",
            live = if (x != null && y != null && y != 0.0)
                "Travelling ${fmt(x)} miles in ${fmt(y)} minutes works out to ${fmt(x / y * 60.0)} mph."
            else "Speed reads ${fmt(mph)} above the MPH index right now."
        )

        // ---------------- Statute miles ↔ km
        Section(
            title = "Miles to kilometres",
            explanation =
                "A statute mile is 1.609 kilometres. The dial has a small red " +
                "STAT marker between 35 and 40 on the inner ring, and a KM " +
                "marker between 60 and 65. Line up your miles value with STAT, " +
                "then read the kilometres at KM.",
            live = if (x != null)
                "${fmt(x)} miles works out to ${fmt(x * DialMath.MILE_TO_KM)} kilometres."
            else "Conversion factor: 1 mile = 1.609 km."
        )

        // ---------------- Nautical miles ↔ km
        Section(
            title = "Nautical miles to kilometres",
            explanation =
                "A nautical mile is 1.852 kilometres — it's used at sea and in " +
                "the air. The NAUT marker (small red triangle near 35 on the " +
                "inner ring) plus the KM marker do the same trick as STAT, but " +
                "for nautical miles instead of land miles.",
            live = if (x != null)
                "${fmt(x)} nautical miles works out to ${fmt(x * DialMath.NAUT_TO_KM)} kilometres."
            else "Conversion factor: 1 nautical mile = 1.852 km."
        )

        // ---------------- Hours / Minutes / Seconds
        Section(
            title = "Hours, minutes and seconds",
            explanation =
                "There are 60 minutes in an hour and 60 seconds in a minute, so " +
                "60 × 60 = 3600 seconds in an hour. The dial has red markers " +
                "right at 60 and at 36 (which stands for 3600). Line up your " +
                "hours on the outer ring against inner-10, then read the " +
                "minutes above inner-60 and the seconds above inner-36 — all in " +
                "one go.",
            live = "${fmt(k)} hours is the same as ${fmt(k * 60)} minutes, " +
                "or ${fmt(k * 3600)} seconds.\n" +
                "On the dial: above inner-60 reads ${fmt(above60)}, " +
                "and above inner-36 reads ${fmt(above36)}."
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
    if (kotlin.math.abs(value - rounded) < 1e-6 && kotlin.math.abs(value) < 1e9) {
        return rounded.toString()
    }
    val s = "%.4f".format(value).trimEnd('0').trimEnd('.')
    return s.ifEmpty { "0" }
}
