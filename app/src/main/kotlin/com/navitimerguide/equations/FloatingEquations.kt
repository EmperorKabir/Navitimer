package com.navitimerguide.equations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * Light-weight labelled equations rendered as plain text rows (no fat
 * cards). Every row reads the current bezel multiplier K from
 * [rotationDegrees] together with the user's typed [outer] and [inner]
 * values, so it updates live whenever the bezel moves OR the user
 * changes a number.
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Live equations", style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold)

        EquationRow(
            label = "Division",
            formula = "X ÷ Y = K",
            worked = if (x != null && y != null && y != 0.0)
                "${fmt(x)} ÷ ${fmt(y)} = ${fmt(x / y)}"
            else "—"
        )
        EquationRow(
            label = "Multiplication",
            formula = "Y × K = X",
            worked = if (x != null && y != null)
                "${fmt(y)} × ${fmt(k)} = ${fmt(y * k)}"
            else "—"
        )
        EquationRow(
            label = "Speed (mph)",
            formula = "(distance ÷ minutes) × 60 = mph",
            worked = if (x != null && y != null && y != 0.0)
                "(${fmt(x)} ÷ ${fmt(y)}) × 60 = ${fmt(x / y * 60.0)} mph"
            else "—"
        )
        EquationRow(
            label = "Statute miles ↔ km",
            formula = "miles × 1.609 = km",
            worked = if (x != null)
                "${fmt(x)} mi = ${fmt(x * DialMath.MILE_TO_KM)} km"
            else "—"
        )
        EquationRow(
            label = "Nautical miles ↔ km",
            formula = "naut. miles × 1.852 = km",
            worked = if (x != null)
                "${fmt(x)} nm = ${fmt(x * DialMath.NAUT_TO_KM)} km"
            else "—"
        )
        EquationRow(
            label = "Hours / minutes / seconds",
            formula = "K h = K × 60 min = K × 3600 s",
            worked = "${fmt(k)} h = ${fmt(k * 60.0)} min = ${fmt(k * 3600.0)} sec"
        )
    }
}

@Composable
private fun EquationRow(label: String, formula: String, worked: String) {
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
        Text(worked, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 1.dp))
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
