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
 * Floating equation rows, one per spreadsheet group. Each row has:
 *   - a TITLE
 *   - a one-line PLAIN-ENGLISH EXPLANATION of how the bezel encodes it
 *   - the abstract FORMULA (X / Y, mph, etc.)
 *   - the LIVE WORKED RESULT computed from the current Outer / Inner inputs
 *     and the bezel-derived multiplier K.
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

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Live equations",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        EquationRow(
            label = "Division",
            explanation = "Line up Outer X with Inner Y on the bezel. The ratio X ÷ Y is the multiplier K, read off above inner-10.",
            formula = "X ÷ Y = K",
            worked = if (x != null && y != null && y != 0.0)
                "${fmt(x)} ÷ ${fmt(y)} = ${fmt(x / y)}  (K = ${fmt(k)})"
            else "—"
        )
        EquationRow(
            label = "Multiplication",
            explanation = "Once K is set, every Outer value is K × the Inner value below it. Pick any Inner Y and read the product above.",
            formula = "Y × K = X",
            worked = if (y != null) "${fmt(y)} × ${fmt(k)} = ${fmt(y * k)}" else "—"
        )
        EquationRow(
            label = "Speed (mph)",
            explanation = "Align distance (miles) on the outer with elapsed time (minutes) on the inner; the value above the red MPH index (inner-60) is the speed.",
            formula = "(Distance ÷ Time) × 60 = mph",
            worked = if (x != null && y != null && y != 0.0)
                "(${fmt(x)} ÷ ${fmt(y)}) × 60 = ${fmt(x / y * 60.0)} mph"
            else "—"
        )
        EquationRow(
            label = "Statute miles ↔ km",
            explanation = "Bezel shortcut: align your number with the STAT marker on the inner scale; read kilometres above the KM marker. Factor 1.609.",
            formula = "miles × 1.609 = km",
            worked = if (x != null) "${fmt(x)} mi  =  ${fmt(x * DialMath.MILE_TO_KM)} km" else "—"
        )
        EquationRow(
            label = "Nautical miles ↔ km",
            explanation = "Same trick, but using the NAUT marker. Factor 1.852.",
            formula = "n.mi × 1.852 = km",
            worked = if (x != null) "${fmt(x)} nm  =  ${fmt(x * DialMath.NAUT_TO_KM)} km" else "—"
        )
        EquationRow(
            label = "Hours / minutes / seconds",
            explanation = "K hours sits at outer-10. Read minutes above inner-60, seconds above inner-36 (because 60 × 60 = 3600).",
            formula = "K h  =  K × 60 min  =  K × 3600 sec",
            worked = "${fmt(k)} h  =  ${fmt(k * 60.0)} min  =  ${fmt(k * 3600.0)} sec"
        )
    }
}

@Composable
private fun EquationRow(label: String, explanation: String, formula: String, worked: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(formula, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(2.dp))
        Text(explanation, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(3.dp))
        Text(worked, style = MaterialTheme.typography.bodyLarge,
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
