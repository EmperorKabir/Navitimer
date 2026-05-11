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
 *
 * Where the watch supports BOTH directions (division ↔ inverse division,
 * multiplication ↔ inverse), each section shows two live readings — one
 * for the primary direction and one for the alternative — so the user
 * can see both interpretations of the current bezel rotation.
 *
 * The km / nautical-km sections read VALUES STRAIGHT FROM THE BEZEL —
 * the outer scale value above each marker — rather than re-applying a
 * textbook conversion factor. That's the point of the slide rule.
 */
@Composable
fun FloatingEquations(
    rotationDegrees: Double,
    outer: String,
    inner: String,
    statRead: String,
    nautRead: String,
    kmRead: String,
    modifier: Modifier = Modifier
) {
    val k = DialMath.multiplierFromRotation(rotationDegrees)
    val invK = if (k > 0 && k.isFinite()) 1.0 / k else Double.NaN
    val x = outer.toDoubleOrNull()
    val y = inner.toDoubleOrNull()
    val mph = DialMath.outerValueAtInner(60.0, rotationDegrees)
    val above60 = DialMath.outerValueAtInner(60.0, rotationDegrees)
    val above36 = DialMath.outerValueAtInner(36.0, rotationDegrees)
    val statVal = statRead.toDoubleOrNull() ?: DialMath.STAT_MARKER
    val nautVal = nautRead.toDoubleOrNull() ?: DialMath.NAUT_MARKER
    val kmVal = kmRead.toDoubleOrNull() ?: DialMath.KM_MARKER

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Live equations", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // ---------------- Division (with alternative direction)
        Section(
            title = "Division",
            primaryExplanation =
                "Outer scale number ÷ inner scale number (with the inner number " +
                "aligned to that outer number) = the outer scale number sitting " +
                "above inner 10.",
            primaryLive = if (x != null && y != null && y != 0.0)
                "Outer ${fmt(x)} ÷ inner ${fmt(y)} = ${fmt(x / y)}."
            else "Type Outer and Inner above to see the live answer.",
            altExplanation =
                "Alternatively: inner scale number ÷ outer scale number (outer " +
                "aligned to that inner number) = the inner scale number sitting " +
                "below outer 10.",
            altLive = if (y != null && x != null && x != 0.0)
                "Inner ${fmt(y)} ÷ outer ${fmt(x)} = ${fmt(y / x)}."
            else null
        )

        // ---------------- Multiplication (with alternative direction)
        Section(
            title = "Multiplication",
            primaryExplanation =
                "Align outer 10 with the inner-scale multiplier. Now any outer " +
                "scale number × that preset multiplier = the inner scale number " +
                "aligned to that outer number.",
            primaryLive = if (y != null)
                "Bezel multiplier is ${fmt(k)}; inner ${fmt(y)} × ${fmt(k)} = ${fmt(y * k)}."
            else "Slide the bezel to set a multiplier; the live value will appear here.",
            altExplanation =
                "Alternatively: align inner 10 with the outer-scale multiplier. " +
                "Now any inner scale number × that preset multiplier = the outer " +
                "scale number aligned to that inner number.",
            altLive = if (x != null && invK.isFinite())
                "Outer ${fmt(x)} × ${fmt(invK)} = ${fmt(x * invK)} on inner."
            else null
        )

        // ---------------- Speed
        Section(
            title = "Speed in miles per hour",
            primaryExplanation =
                "Speed is distance ÷ time, scaled to per hour. Line up your " +
                "distance in miles on the outer ring with how long it took in " +
                "minutes on the inner ring. The mph reading appears above the " +
                "12 o'clock MPH index.",
            primaryLive = if (x != null && y != null && y != 0.0)
                "${fmt(x)} ${unit(x, "mile", "miles")} in ${fmt(y)} " +
                "${unit(y, "minute", "minutes")} = ${fmt(x / y * 60.0)} mph."
            else "MPH index reads ${fmt(mph)} mph at the current rotation.",
            altExplanation = null,
            altLive = null
        )

        // ---------------- Time
        Section(
            title = "Time for a journey",
            primaryExplanation =
                "With a speed already on the dial, the bezel will tell you how " +
                "long any distance will take — read the inner value below any " +
                "outer-ring distance.",
            primaryLive = if (x != null && mph.isFinite() && mph > 0)
                "At ${fmt(mph)} mph, ${fmt(x)} ${unit(x, "mile", "miles")} takes " +
                "${fmt(x * 60.0 / mph)} ${unit(x * 60.0 / mph, "minute", "minutes")}."
            else "Set a speed on the dial to see how long a distance takes.",
            altExplanation = null,
            altLive = null
        )

        // ---------------- Statute miles ↔ km
        Section(
            title = "Miles to kilometres",
            primaryExplanation =
                "Line up your miles value on the outer ring with the small red " +
                "STAT triangle. Read the kilometres above the KM marker.",
            primaryLive =
                "${fmt(statVal)} ${unit(statVal, "statute mile", "statute miles")} = " +
                "${fmt(kmVal)} ${unit(kmVal, "kilometre", "kilometres")}.",
            altExplanation = null,
            altLive = null
        )

        // ---------------- Nautical miles ↔ km
        Section(
            title = "Nautical miles to kilometres",
            primaryExplanation =
                "Line up your nautical-mile value with the NAUT triangle. Read " +
                "the kilometres above the KM marker — same trick as STAT, but " +
                "for sea / air distances.",
            primaryLive =
                "${fmt(nautVal)} ${unit(nautVal, "nautical mile", "nautical miles")} = " +
                "${fmt(kmVal)} ${unit(kmVal, "kilometre", "kilometres")}.",
            altExplanation = null,
            altLive = null
        )

        // ---------------- Hours / Minutes / Seconds (with alternative)
        Section(
            title = "Hours, minutes and seconds",
            primaryExplanation =
                "60 seconds in a minute, 60 minutes in an hour — so 60 × 60 = " +
                "3600 seconds per hour. That's why the red 36 markers matter: " +
                "they stand for 3600 (seconds-per-hour). To convert hours into " +
                "minutes and seconds, align your hours-times-10 on the outer " +
                "scale against inner 10 (the unit index). Above inner 60 reads " +
                "the minutes (÷ 10); above inner 36 reads the seconds (÷ 100). " +
                "Worked example: align outer 40 (= 4 hours) with inner 10 — " +
                "above inner 60 reads 24 (= 240 minutes), and above inner 36 " +
                "reads 14.4 (= 14 400 seconds).",
            primaryLive = "${fmt(k)} ${unit(k, "hour", "hours")} = " +
                "${fmt(k * 60)} ${unit(k * 60, "minute", "minutes")} = " +
                "${fmt(k * 3600)} ${unit(k * 3600, "second", "seconds")}.",
            altExplanation =
                "Alternatively: both inner and outer carry the red 36 and 60 " +
                "markers, so you can invert the calculation just like division " +
                "and multiplication — drive it from any of the three anchors " +
                "(10 / 60 / 36) and the bezel keeps the others in sync.",
            altLive =
                "Bezel reads above inner 60: ${fmt(above60)}; above inner 36: ${fmt(above36)}."
        )
    }
}

@Composable
private fun Section(
    title: String,
    primaryExplanation: String,
    primaryLive: String,
    altExplanation: String?,
    altLive: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(2.dp))
        Text(
            primaryExplanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(4.dp))
        Text(
            primaryLive, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        if (altExplanation != null) {
            Spacer(Modifier.size(8.dp))
            Text(
                altExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (altLive != null) {
                Spacer(Modifier.size(4.dp))
                Text(
                    altLive, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
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

private fun unit(value: Double, singular: String, plural: String): String {
    val isOne = abs(value - 1.0) < 1e-6
    return if (isOne) singular else plural
}
