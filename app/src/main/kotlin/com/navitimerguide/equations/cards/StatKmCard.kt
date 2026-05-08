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
import com.navitimerguide.dial.DialMath

@Composable
fun StatKmCard(
    onSnapAlign: (outerX: Double, innerY: Double) -> Unit
) {
    var miles by remember { mutableStateOf("10") }
    var km by remember { mutableStateOf("16.09") }

    fun reset() { miles = "10"; km = "16.09" }

    EquationCardScaffold(
        title = "Statute miles ↔ Kilometres",
        hint = "Align miles on the outer scale with the STAT marker on the inner; read km above the KM marker. 1 mile = 1.609 km.",
        onReset = ::reset
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EquationField(
                text = miles, label = "Miles", suffix = "mi",
                onChange = { v ->
                    miles = v
                    v.toDoubleOrNull()?.let { km = fmt(it * DialMath.MILE_TO_KM) }
                },
                onCommit = {
                    miles.toDoubleOrNull()?.let { onSnapAlign(it, DialMath.STAT_MARKER) }
                },
                modifier = Modifier.weight(1f)
            )
            Text("↔", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = km, label = "Kilometres", suffix = "km",
                onChange = { v ->
                    km = v
                    v.toDoubleOrNull()?.let { miles = fmt(it / DialMath.MILE_TO_KM) }
                },
                onCommit = {
                    miles.toDoubleOrNull()?.let { onSnapAlign(it, DialMath.STAT_MARKER) }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
