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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * The two and only two number inputs: one for the OUTER (rotating) bezel
 * value, one for the INNER (fixed) chapter-ring value. On commit (Done /
 * focus loss), the bezel snaps so outer-X sits on top of inner-Y.
 *
 * The floating equations panel below reads from the bezel rotation, so
 * it updates whenever the bezel moves — whether by these inputs, by the
 * presets, by the slider, or by direct drag.
 */
@Composable
fun BezelInputs(
    outer: String,
    inner: String,
    onOuterChange: (String) -> Unit,
    onInnerChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            "Align two scales",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.size(2.dp))
        Text(
            "Type a value for each scale; the bezel snaps so they line up.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = outer,
                onValueChange = { raw ->
                    onOuterChange(raw.filter { it.isDigit() || it == '.' })
                },
                label = { Text("Outer bezel") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = inner,
                onValueChange = { raw ->
                    onInnerChange(raw.filter { it.isDigit() || it == '.' })
                },
                label = { Text("Inner bezel") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onCommit() })
            )
        }
    }
}
