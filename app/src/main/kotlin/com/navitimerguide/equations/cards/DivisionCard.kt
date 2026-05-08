package com.navitimerguide.equations.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DivisionCard(
    onSnapAlign: (outerX: Double, innerY: Double) -> Unit,
    onSetMultiplier: (Double) -> Unit
) {
    var x by remember { mutableStateOf("20") }
    var y by remember { mutableStateOf("8") }
    var z by remember { mutableStateOf("2.5") }

    fun reset() { x = "20"; y = "8"; z = "2.5" }

    EquationCardScaffold(
        title = "Division",
        hint = "Align outer X with inner Y on the bezel; the ratio (X ÷ Y) reads above inner 10.",
        onReset = ::reset
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EquationField(
                text = x, label = "X",
                onChange = { v ->
                    x = v
                    val xd = v.toDoubleOrNull(); val yd = y.toDoubleOrNull()
                    if (xd != null && yd != null && yd != 0.0) z = fmt(xd / yd)
                },
                onCommit = {
                    val xd = x.toDoubleOrNull(); val yd = y.toDoubleOrNull()
                    if (xd != null && yd != null && yd > 0) onSnapAlign(xd, yd)
                },
                modifier = Modifier.weight(1f)
            )
            Text("÷", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = y, label = "Y",
                onChange = { v ->
                    y = v
                    val xd = x.toDoubleOrNull(); val yd = v.toDoubleOrNull()
                    if (xd != null && yd != null && yd != 0.0) z = fmt(xd / yd)
                },
                onCommit = {
                    val xd = x.toDoubleOrNull(); val yd = y.toDoubleOrNull()
                    if (xd != null && yd != null && yd > 0) onSnapAlign(xd, yd)
                },
                modifier = Modifier.weight(1f)
            )
            Text("=", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = z, label = "Z",
                onChange = { v ->
                    z = v
                    val zd = v.toDoubleOrNull(); val yd = y.toDoubleOrNull()
                    if (zd != null && yd != null) x = fmt(yd * zd)
                },
                onCommit = {
                    val zd = z.toDoubleOrNull()
                    if (zd != null && zd > 0) onSetMultiplier(zd)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
