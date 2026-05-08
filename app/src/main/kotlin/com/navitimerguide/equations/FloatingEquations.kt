package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * Equations panel mirrors the source-of-truth spreadsheet exactly.
 * Each section has:
 *   • a TITLE
 *   • the spreadsheet's RECIPE (how to perform the operation on the bezel)
 *   • the LIVE READING (what the bezel currently shows)
 *
 * The wording is taken from the spreadsheet so the user can read this
 * panel and see the exact instructions for what each bezel alignment is
 * doing. Live numbers come from the current rotation + the user's typed
 * Outer / Inner anchors.
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
    // Live mph above MPH index (inner-60)
    val mphReading = DialMath.outerValueAtInner(60.0, rotationDegrees)
    // Live time-conversion readings: with bezel rotated for "K hours",
    // outer above inner-60 = K*6 (=> K*60 minutes); above inner-36 = K*3.6 (=> K*3600 seconds)
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
            recipe = listOf(
                "Outer scale number ÷ Inner scale number (aligned to it) = Outer scale number above inner-10",
                "Alternatively: Inner number ÷ Outer number = Inner number below outer-10"
            ),
            live = if (x != null && y != null && y != 0.0)
                "Now: ${fmt(x)} ÷ ${fmt(y)} = ${fmt(x / y)}    (= K, the bezel multiplier = ${fmt(k)})"
            else "Now: type Outer & Inner above to see the live division"
        )

        // ---------------- Multiplication
        Section(
            title = "Multiplication",
            recipe = listOf(
                "Align outer-10 to the multiplier on the inner scale.",
                "Now: any outer number × multiplier = inner number aligned to it.",
                "Alternatively: align inner-10 to a multiplier on the outer scale; any inner × multiplier = outer aligned."
            ),
            live = "Now: multiplier K = ${fmt(k)}" +
                if (y != null) "    e.g. ${fmt(y)} × ${fmt(k)} = ${fmt(y * k)}" else ""
        )

        // ---------------- Speed / Time / Distance
        Section(
            title = "Speed / Time / Distance",
            recipe = listOf(
                "Speed: outer (miles) aligned with inner (minutes) → mph reads above the MPH index (inner red 60 at 12 o'clock).",
                "Time: outer above MPH aligned with outer (miles) → reads inner (minutes).",
                "Tip: outer = miles, inner = time, mph reads at red 60."
            ),
            live = if (x != null && y != null && y != 0.0)
                "Now: ${fmt(x)} mi in ${fmt(y)} min → above MPH = ${fmt(mphReading)} mph"
            else "Now: above MPH (inner 60) = ${fmt(mphReading)}"
        )

        // ---------------- Statute miles → km
        Section(
            title = "Statute miles ↔ kilometres",
            recipe = listOf(
                "Align your number to the red STAT marker (just before the 40 on the inner scale).",
                "Read the kilometre value at the KM marker. 1 mile = 1.609 km."
            ),
            live = if (x != null) "Now: ${fmt(x)} mi = ${fmt(x * DialMath.MILE_TO_KM)} km"
                   else "Now: 1 mi = 1.609 km"
        )

        // ---------------- Nautical miles → km
        Section(
            title = "Nautical miles ↔ kilometres",
            recipe = listOf(
                "Same recipe but using the NAUT marker (lower red arrow, just before the 35).",
                "1 nautical mile = 1.852 km."
            ),
            live = if (x != null) "Now: ${fmt(x)} nm = ${fmt(x * DialMath.NAUT_TO_KM)} km"
                   else "Now: 1 nm = 1.852 km"
        )

        // ---------------- Time units via the red 36 marker
        Section(
            title = "Hours / Minutes / Seconds (the red 36 marker)",
            recipe = listOf(
                "Red markers at 36 on both scales — significant because 60 sec/min × 60 min/hr = 3600.",
                "Recipe: align your hours value (× 10 on outer) to inner-10.",
                "Above inner-60 → minutes (×60).  Above inner-36 → seconds (×3600).",
                "Worked: 4 hours → outer-40 over inner-10 → above 60 = 24 (240 min); above 36 = 14.4 (14,400 sec)."
            ),
            live = "Now: K = ${fmt(k)} h" +
                "    above inner-60 = ${fmt(above60)} (= ${fmt(k * 60)} min)" +
                "    above inner-36 = ${fmt(above36)} (= ${fmt(k * 3600)} sec)"
        )
    }
}

@Composable
private fun Section(title: String, recipe: List<String>, live: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.size(2.dp))
        recipe.forEach { line ->
            Text("• $line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
