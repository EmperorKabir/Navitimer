package com.navitimerguide.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun StepArrows(
    onStep: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text("± step the bezel", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.size(6.dp))
        listOf(0.1, 1.0, 10.0).forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("± ${"%.1f".format(step)}°", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Row {
                    FilledTonalIconButton(onClick = { onStep(-step) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "rotate -${step}°")
                    }
                    Spacer(Modifier.size(8.dp))
                    FilledTonalIconButton(onClick = { onStep(step) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "rotate +${step}°")
                    }
                }
            }
        }
    }
}
