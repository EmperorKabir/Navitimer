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
fun TimeUnitsCard(
    onSetMultiplier: (Double) -> Unit
) {
    var hours by remember { mutableStateOf("4") }
    var minutes by remember { mutableStateOf("240") }
    var seconds by remember { mutableStateOf("14400") }

    fun reset() { hours = "4"; minutes = "240"; seconds = "14400" }

    fun deriveFromHours(v: String) {
        v.toDoubleOrNull()?.let {
            minutes = fmt(it * 60.0)
            seconds = fmt(it * 3600.0)
        }
    }
    fun deriveFromMinutes(v: String) {
        v.toDoubleOrNull()?.let {
            hours = fmt(it / 60.0)
            seconds = fmt(it * 60.0)
        }
    }
    fun deriveFromSeconds(v: String) {
        v.toDoubleOrNull()?.let {
            hours = fmt(it / 3600.0)
            minutes = fmt(it / 60.0)
        }
    }

    EquationCardScaffold(
        title = "Hours / Minutes / Seconds",
        hint = "Align hours to outer 10 — read minutes above inner 60, seconds above inner 36 (because 60 × 60 = 3600).",
        onReset = ::reset
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EquationField(
                text = hours, label = "Hours", suffix = "h",
                onChange = { v -> hours = v; deriveFromHours(v) },
                onCommit = { hours.toDoubleOrNull()?.let { onSetMultiplier(it) } },
                modifier = Modifier.weight(1f)
            )
            Text("=", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = minutes, label = "Minutes", suffix = "min",
                onChange = { v -> minutes = v; deriveFromMinutes(v) },
                onCommit = {
                    minutes.toDoubleOrNull()?.let { onSetMultiplier(it / 60.0) }
                },
                modifier = Modifier.weight(1f)
            )
            Text("=", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = seconds, label = "Seconds", suffix = "sec",
                onChange = { v -> seconds = v; deriveFromSeconds(v) },
                onCommit = {
                    seconds.toDoubleOrNull()?.let { onSetMultiplier(it / 3600.0) }
                },
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}
