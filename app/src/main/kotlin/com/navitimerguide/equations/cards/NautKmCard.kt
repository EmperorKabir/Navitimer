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
fun NautKmCard(
    onSnapAlign: (outerX: Double, innerY: Double) -> Unit
) {
    var nm by remember { mutableStateOf("10") }
    var km by remember { mutableStateOf("18.52") }

    fun reset() { nm = "10"; km = "18.52" }

    EquationCardScaffold(
        title = "Nautical miles ↔ Kilometres",
        hint = "Same as STAT, but using the NAUT marker. 1 nautical mile = 1.852 km.",
        onReset = ::reset
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EquationField(
                text = nm, label = "Nautical miles", suffix = "nm",
                onChange = { v ->
                    nm = v
                    v.toDoubleOrNull()?.let { km = fmt(it * DialMath.NAUT_TO_KM) }
                },
                onCommit = {
                    nm.toDoubleOrNull()?.let { onSnapAlign(it, DialMath.NAUT_MARKER) }
                },
                modifier = Modifier.weight(1f)
            )
            Text("↔", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = km, label = "Kilometres", suffix = "km",
                onChange = { v ->
                    km = v
                    v.toDoubleOrNull()?.let { nm = fmt(it / DialMath.NAUT_TO_KM) }
                },
                onCommit = {
                    nm.toDoubleOrNull()?.let { onSnapAlign(it, DialMath.NAUT_MARKER) }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
