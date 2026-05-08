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
fun SpeedTimeDistanceCard(
    onSnapAlign: (outerX: Double, innerY: Double) -> Unit
) {
    var distance by remember { mutableStateOf("120") }
    var time by remember { mutableStateOf("90") }
    var speed by remember { mutableStateOf("80") }

    fun reset() { distance = "120"; time = "90"; speed = "80" }

    fun derive(d: String, t: String, s: String, edited: Char) {
        val dd = d.toDoubleOrNull(); val td = t.toDoubleOrNull(); val sd = s.toDoubleOrNull()
        when (edited) {
            'd' -> if (dd != null && td != null && td != 0.0) speed = fmt(dd / td * 60.0)
            't' -> if (dd != null && td != null && td != 0.0) speed = fmt(dd / td * 60.0)
            's' -> if (sd != null && td != null) distance = fmt(sd * td / 60.0)
            else -> {}
        }
    }

    EquationCardScaffold(
        title = "Speed / Time / Distance",
        hint = "Align distance (outer) with time-in-minutes (inner). Speed reads above the MPH index (inner 60). mph = (distance ÷ minutes) × 60.",
        onReset = ::reset
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EquationField(
                text = distance, label = "Distance", suffix = "mi",
                onChange = { v -> distance = v; derive(v, time, speed, 'd') },
                onCommit = {
                    val dd = distance.toDoubleOrNull(); val td = time.toDoubleOrNull()
                    if (dd != null && td != null && dd > 0 && td > 0) onSnapAlign(dd, td)
                },
                modifier = Modifier.weight(1.1f)
            )
            Text("÷", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = time, label = "Time", suffix = "min",
                onChange = { v -> time = v; derive(distance, v, speed, 't') },
                onCommit = {
                    val dd = distance.toDoubleOrNull(); val td = time.toDoubleOrNull()
                    if (dd != null && td != null && dd > 0 && td > 0) onSnapAlign(dd, td)
                },
                modifier = Modifier.weight(0.9f)
            )
            Text("=", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = speed, label = "Speed", suffix = "mph",
                onChange = { v -> speed = v; derive(distance, time, v, 's') },
                onCommit = {
                    val dd = distance.toDoubleOrNull(); val td = time.toDoubleOrNull()
                    if (dd != null && td != null && dd > 0 && td > 0) onSnapAlign(dd, td)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
