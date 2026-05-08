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
fun MultiplicationCard(
    onSetMultiplier: (Double) -> Unit
) {
    var a by remember { mutableStateOf("4") }
    var k by remember { mutableStateOf("2.5") }
    var b by remember { mutableStateOf("10") }

    fun reset() { a = "4"; k = "2.5"; b = "10" }

    EquationCardScaffold(
        title = "Multiplication",
        hint = "Align outer 10 with inner K on the bezel; B reads above outer A (B = A × K).",
        onReset = ::reset
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EquationField(
                text = a, label = "A",
                onChange = { v ->
                    a = v
                    val ad = v.toDoubleOrNull(); val kd = k.toDoubleOrNull()
                    if (ad != null && kd != null) b = fmt(ad * kd)
                },
                onCommit = {
                    val ad = a.toDoubleOrNull(); val bd = b.toDoubleOrNull()
                    if (ad != null && bd != null && ad != 0.0) onSetMultiplier(bd / ad)
                },
                modifier = Modifier.weight(1f)
            )
            Text("×", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = k, label = "K",
                onChange = { v ->
                    k = v
                    val ad = a.toDoubleOrNull(); val kd = v.toDoubleOrNull()
                    if (ad != null && kd != null) b = fmt(ad * kd)
                },
                onCommit = {
                    val kd = k.toDoubleOrNull()
                    if (kd != null && kd > 0) onSetMultiplier(kd)
                },
                modifier = Modifier.weight(1f)
            )
            Text("=", style = MaterialTheme.typography.titleLarge)
            EquationField(
                text = b, label = "B",
                onChange = { v ->
                    b = v
                    val bd = v.toDoubleOrNull(); val ad = a.toDoubleOrNull()
                    if (bd != null && ad != null && ad != 0.0) k = fmt(bd / ad)
                },
                onCommit = {
                    val ad = a.toDoubleOrNull(); val bd = b.toDoubleOrNull()
                    if (ad != null && bd != null && ad > 0) onSetMultiplier(bd / ad)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
