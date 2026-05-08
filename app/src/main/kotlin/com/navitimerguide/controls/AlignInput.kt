package com.navitimerguide.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AlignInput(
    onSnapAlign: (Double, Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var outer by remember { mutableStateOf("10") }
    var inner by remember { mutableStateOf("16.09") }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            "Align outer X to inner Y",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = outer,
                onValueChange = { outer = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Outer X") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(8.dp))
            Text("→", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = inner,
                onValueChange = { inner = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Inner Y") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onReset) { Text("Reset") }
            Spacer(Modifier.size(8.dp))
            Button(onClick = {
                val x = outer.toDoubleOrNull()
                val y = inner.toDoubleOrNull()
                if (x != null && y != null) onSnapAlign(x, y)
            }) { Text("Snap") }
        }
    }
}
